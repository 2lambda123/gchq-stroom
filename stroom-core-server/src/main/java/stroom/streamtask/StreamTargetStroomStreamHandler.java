/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.streamtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.data.store.StreamFactory;
import stroom.data.store.api.OutputStreamProvider;
import stroom.data.store.api.Store;
import stroom.data.store.api.Target;
import stroom.feed.shared.FeedDoc;
import stroom.meta.api.AttributeMapUtil;
import stroom.meta.shared.AttributeMap;
import stroom.meta.shared.Meta;
import stroom.meta.shared.MetaProperties;
import stroom.meta.shared.StandardHeaderArguments;
import stroom.pipeline.feed.FeedDocCache;
import stroom.proxy.repo.StroomHeaderStreamHandler;
import stroom.proxy.repo.StroomStreamHandler;
import stroom.proxy.repo.StroomZipEntry;
import stroom.proxy.repo.StroomZipFileType;
import stroom.proxy.repo.StroomZipNameSet;
import stroom.streamstore.shared.StreamTypeNames;
import stroom.streamtask.statistic.MetaDataStatistic;
import stroom.util.io.CloseableUtil;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Type of {@link StroomStreamHandler} that store the entries in the stream store.
 * There are some special rules about how this works.
 * <p>
 * This is fine if all the meta data indicates they belong to the same feed
 * 001.meta, 002.meta, 001.dat, 002.dat
 * <p>
 * This is also fine if 001.meta indicates 001 belongs to feed X and 002.meta
 * indicates 001 belongs to feed Y 001.meta, 002.meta, 001.dat, 002.dat
 * <p>
 * However if the global header map indicates feed Z and the files are send in
 * the following order 001.dat, 002.dat, 001.meta, 002.meta this is invalid ....
 * I.E. as soon as we add non header stream for a feed if the header turns out
 * to be different we must throw an exception.
 */
public class StreamTargetStroomStreamHandler implements StroomStreamHandler, StroomHeaderStreamHandler, Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamTargetStroomStreamHandler.class);

    private final Store streamStore;
    private final FeedDocCache feedDocCache;
    private final MetaDataStatistic metaDataStatistics;
    private final HashSet<Meta> streamSet;
    private final StroomZipNameSet stroomZipNameSet;
    //    private final Map<String, OutputStreamProvider> outputStreamProviderMap = new HashMap<>();
    private final Map<String, Target> targetMap = new HashMap<>();
    //    private final Map<String, Target> feedStreamTarget = new HashMap<>();
    private final ByteArrayOutputStream currentHeaderByteArrayOutputStream = new ByteArrayOutputStream();
    private boolean oneByOne;
    private StroomZipFileType currentFileType = null;
    private StroomZipEntry currentStroomZipEntry = null;
    private StroomZipEntry lastDatStroomZipEntry = null;
    private StroomZipEntry lastCtxStroomZipEntry = null;
    private String currentFeedName;
    private String currentStreamTypeName;
    private AttributeMap globalAttributeMap;
    private AttributeMap currentAttributeMap;

    private OutputStreamProvider currentOutputStreamProvider;
    private OutputStream currentOutputStream;

    public StreamTargetStroomStreamHandler(final Store streamStore,
                                           final FeedDocCache feedDocCache,
                                           final MetaDataStatistic metaDataStatistics,
                                           final String feedName,
                                           final String streamTypeName) {
        this.streamStore = streamStore;
        this.feedDocCache = feedDocCache;
        this.metaDataStatistics = metaDataStatistics;
        this.currentFeedName = feedName;
        this.currentStreamTypeName = streamTypeName;
        this.streamSet = new HashSet<>();
        this.stroomZipNameSet = new StroomZipNameSet(true);
    }

    public static List<StreamTargetStroomStreamHandler> buildSingleHandlerList(final Store streamStore,
                                                                               final FeedDocCache feedDocCache,
                                                                               final MetaDataStatistic metaDataStatistics,
                                                                               final String feedName,
                                                                               final String streamTypeName) {
        final ArrayList<StreamTargetStroomStreamHandler> list = new ArrayList<>();
        list.add(new StreamTargetStroomStreamHandler(streamStore, feedDocCache, metaDataStatistics, feedName, streamTypeName));
        return list;
    }

    void setOneByOne(final boolean oneByOne) {
        this.oneByOne = oneByOne;
    }

    @Override
    public void handleHeader(final AttributeMap attributeMap) {
        globalAttributeMap = attributeMap;
    }

    @Override
    public void handleEntryData(final byte[] data, final int off, final int len) throws IOException {
        if (currentFileType.equals(StroomZipFileType.Meta)) {
            currentHeaderByteArrayOutputStream.write(data, off, len);
        } else {
            if (currentOutputStream != null) {
                currentOutputStream.write(data, off, len);
            }
        }
    }

    @Override
    public void handleEntryStart(final StroomZipEntry stroomZipEntry) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("handleEntryStart() - " + stroomZipEntry);
        }

        // Ensure we close the current output stream.
        closeCurrentOutput();

        currentFileType = stroomZipEntry.getStroomZipFileType();
        final String streamTypeName = convertType(currentFileType);

        // We don't want to aggregate reference feeds.
        final boolean singleEntry = isReference(currentFeedName) || oneByOne;

        final StroomZipEntry nextEntry = stroomZipNameSet.add(stroomZipEntry.getFullName());

        if (singleEntry && currentStroomZipEntry != null && !nextEntry.equalsBaseName(currentStroomZipEntry)) {
            // Close it if we have opened it.
            if (targetMap.containsKey(currentFeedName)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("handleEntryStart() - Closing due to singleEntry=" + singleEntry + " " + currentFeedName
                            + " currentStroomZipEntry=" + currentStroomZipEntry + " nextEntry=" + nextEntry);
                }
                closeCurrentFeed();
            }
        }

        currentStroomZipEntry = nextEntry;

        if (StroomZipFileType.Meta.equals(currentFileType)) {
            // Header we just buffer up
            currentHeaderByteArrayOutputStream.reset();
        } else if (StroomZipFileType.Data.equals(currentFileType) || StroomZipFileType.Context.equals(currentFileType)) {
            currentOutputStreamProvider = getTarget().next();
            currentOutputStream = currentOutputStreamProvider.get(streamTypeName);
        }
    }

    private String convertType(StroomZipFileType type) {
        if (type == null || StroomZipFileType.Data.equals(type)) {
            return null;
        }
        switch (type) {
            case Meta:
                return StreamTypeNames.META;
            case Context:
                return StreamTypeNames.CONTEXT;
        }
        return null;
    }

    private String getStreamTypeName(final String feedName) {
        return feedDocCache.get(feedName)
                .map(FeedDoc::getStreamType)
                .orElse(StreamTypeNames.RAW_EVENTS);
    }

    private boolean isReference(final String feedName) {
        return feedDocCache.get(feedName)
                .map(FeedDoc::isReference)
                .orElse(false);
    }

    @Override
    public void handleEntryEnd() throws IOException {
        final String streamTypeName = convertType(currentFileType);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("handleEntryEnd() - " + currentFileType);
        }

        if (StroomZipFileType.Meta.equals(currentFileType)) {
            final byte[] headerBytes = currentHeaderByteArrayOutputStream.toByteArray();
            currentAttributeMap = null;
            if (globalAttributeMap != null) {
                currentAttributeMap = AttributeMapUtil.cloneAllowable(globalAttributeMap);
            } else {
                currentAttributeMap = new AttributeMap();
            }
            AttributeMapUtil.read(headerBytes, currentAttributeMap);

            if (metaDataStatistics != null) {
                metaDataStatistics.recordStatistics(currentAttributeMap);
            }

            // Are we switching feed?
            final String feedName = currentAttributeMap.get(StandardHeaderArguments.FEED);
            if (feedName != null) {
                if (currentFeedName == null || !currentFeedName.equals(feedName)) {
                    // Yes ... load the new feed
                    currentFeedName = feedName;
                    currentStreamTypeName = getStreamTypeName(currentFeedName);

                    final String currentBaseName = currentStroomZipEntry.getBaseName();

                    // Have we stored some data or context
                    if (lastDatStroomZipEntry != null
                            && stroomZipNameSet.getBaseName(lastDatStroomZipEntry.getFullName()).equals(currentBaseName)) {
                        throw new IOException("Header and Data out of order for multiple feed data");
                    }
                    if (lastCtxStroomZipEntry != null
                            && stroomZipNameSet.getBaseName(lastCtxStroomZipEntry.getFullName()).equals(currentBaseName)) {
                        throw new IOException("Header and Data out of order for multiple feed data");
                    }
                }
            }

            try (final OutputStream outputStream = currentOutputStreamProvider.get(streamTypeName)) {
                outputStream.write(headerBytes);
            }
        }

        // Ensure we close the current output stream.
        closeCurrentOutput();

        if (StroomZipFileType.Data.equals(currentFileType)) {
            lastDatStroomZipEntry = currentStroomZipEntry;
        }
        if (StroomZipFileType.Context.equals(currentFileType)) {
            lastCtxStroomZipEntry = currentStroomZipEntry;
        }
    }

    private void closeCurrentOutput() {
        try {
            if (currentOutputStream != null) {
                currentOutputStream.close();
                currentOutputStream = null;
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void closeDelete() {
        targetMap.values().forEach(streamStore::deleteStreamTarget);
        targetMap.clear();
    }

    @Override
    public void close() {
        targetMap.values().forEach(CloseableUtil::closeLogAndIgnoreException);

        targetMap.clear();
    }

    private void closeCurrentFeed() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("closeCurrentFeed() - " + currentFeedName);
        }
        CloseableUtil.closeLogAndIgnoreException(targetMap.remove(currentFeedName));
    }

    public Set<Meta> getStreamSet() {
        return Collections.unmodifiableSet(streamSet);
    }

    private AttributeMap getCurrentAttributeMap() {
        if (currentAttributeMap != null) {
            return currentAttributeMap;
        }
        return globalAttributeMap;
    }

//    private void nextOutputStream(final String feedName, final StroomZipFileType stroomZipFileType) {
//        final OutputStream outputStream = getOutputStreamProvider().next();
//        targetMap.put(currentFeedName, outputStream);
//    }
//
//    private void nextOutputStream(final String type) {
//        final OutputStream outputStream = getOutputStreamProvider().next(type);
//        targetMap.put(currentFeedName, outputStream);
//    }
//
//    private OutputStream getOutputStream(final String feedName, final StroomZipFileType stroomZipFileType) {
//        return targetMap.get(currentFeedName);
//    }

    private Target getTarget() {
        return targetMap.computeIfAbsent(currentFeedName, k -> {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("getOutputStreamProvider() - open stream for " + currentFeedName);
            }

            // Get the effective time if one has been provided.
            final Long effectiveMs = StreamFactory.getReferenceEffectiveTime(getCurrentAttributeMap(), true);

            // Make sure the stream type is not null.
            if (currentStreamTypeName == null) {
                currentStreamTypeName = getStreamTypeName(currentFeedName);
            }

            final MetaProperties metaProperties = new MetaProperties.Builder()
                    .feedName(currentFeedName)
                    .typeName(currentStreamTypeName)
                    .effectiveMs(effectiveMs)
                    .build();

            final Target streamTarget = streamStore.openStreamTarget(metaProperties);
            streamSet.add(streamTarget.getMeta());

            return streamTarget;

//            feedStreamTarget.put(currentFeedName, streamTarget);
//
////            final OutputStreamProvider outputStreamProvider = streamTarget.getOutputStreamProvider();
//            return new OutputStreamSet(streamTarget);
//

//            outputStreamProviderMap.put(currentFeedName, outputStreamProvider);

        });

//        OutputStreamProvider outputStreamProvider = outputStreamProviderMap.get(currentFeedName);
//
//        if (outputStreamProvider == null) {
//            if (LOGGER.isDebugEnabled()) {
//                LOGGER.debug("getOutputStreamProvider() - open stream for " + currentFeedName);
//            }
//
//            // Get the effective time if one has been provided.
//            final Long effectiveMs = StreamFactory.getReferenceEffectiveTime(getCurrentAttributeMap(), true);
//
//            // Make sure the stream type is not null.
//            if (currentStreamTypeName == null) {
//                currentStreamTypeName = getStreamTypeName(currentFeedName);
//            }
//
//            final DataProperties metaProperties = new DataProperties.Builder()
//                    .feedName(currentFeedName)
//                    .typeName(currentStreamTypeName)
//                    .effectiveMs(effectiveMs)
//                    .build();
//
//            final StreamTarget streamTarget = streamStore.openStreamTarget(metaProperties);
//            feedStreamTarget.put(currentFeedName, streamTarget);
//            streamSet.add(streamTarget.getMeta());
//            outputStreamProvider = streamTarget.getOutputStreamProvider();
//            outputStreamProviderMap.put(currentFeedName, outputStreamProvider);
//        }
//        return outputStreamProvider;
    }
}
