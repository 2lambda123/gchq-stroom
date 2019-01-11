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

package stroom.headless;

import stroom.data.meta.api.AttributeMap;
import stroom.data.meta.api.Data;
import stroom.docref.DocRef;
import stroom.feed.AttributeMapUtil;
import stroom.feed.FeedStore;
import stroom.feed.StroomHeaderArguments;
import stroom.feed.shared.FeedDoc;
import stroom.pipeline.ErrorWriter;
import stroom.pipeline.ErrorWriterProxy;
import stroom.pipeline.PipelineStore;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.errorhandler.ErrorStatistics;
import stroom.pipeline.errorhandler.LoggedException;
import stroom.pipeline.errorhandler.ProcessException;
import stroom.pipeline.errorhandler.RecordErrorReceiver;
import stroom.pipeline.factory.Pipeline;
import stroom.pipeline.factory.PipelineDataCache;
import stroom.pipeline.factory.PipelineFactory;
import stroom.pipeline.shared.PipelineDoc;
import stroom.pipeline.shared.data.PipelineData;
import stroom.pipeline.state.FeedHolder;
import stroom.pipeline.state.MetaData;
import stroom.pipeline.state.MetaDataHolder;
import stroom.pipeline.state.PipelineHolder;
import stroom.pipeline.state.StreamHolder;
import stroom.pipeline.task.StreamMetaDataProvider;
import stroom.security.Security;
import stroom.streamstore.shared.StreamTypeNames;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.date.DateUtil;
import stroom.util.io.IgnoreCloseInputStream;
import stroom.util.shared.Severity;
import stroom.util.shared.VoidResult;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


class CliTranslationTaskHandler extends AbstractTaskHandler<CliTranslationTask, VoidResult> {
    private final PipelineFactory pipelineFactory;
    private final FeedStore feedStore;
    private final PipelineStore pipelineStore;
    private final MetaData metaData;
    private final PipelineHolder pipelineHolder;
    private final FeedHolder feedHolder;
    private final MetaDataHolder metaDataHolder;
    private final ErrorReceiverProxy errorReceiverProxy;
    private final ErrorWriterProxy errorWriterProxy;
    private final RecordErrorReceiver recordErrorReceiver;
    private final PipelineDataCache pipelineDataCache;
    private final StreamHolder streamHolder;
    private final Security security;

    @Inject
    CliTranslationTaskHandler(final PipelineFactory pipelineFactory,
                              final FeedStore feedStore,
                              final PipelineStore pipelineStore,
                              final MetaData metaData,
                              final PipelineHolder pipelineHolder,
                              final FeedHolder feedHolder,
                              final MetaDataHolder metaDataHolder,
                              final ErrorReceiverProxy errorReceiverProxy,
                              final ErrorWriterProxy errorWriterProxy,
                              final RecordErrorReceiver recordErrorReceiver,
                              final PipelineDataCache pipelineDataCache,
                              final StreamHolder streamHolder,
                              final Security security) {
        this.pipelineFactory = pipelineFactory;
        this.feedStore = feedStore;
        this.pipelineStore = pipelineStore;
        this.metaData = metaData;
        this.pipelineHolder = pipelineHolder;
        this.feedHolder = feedHolder;
        this.metaDataHolder = metaDataHolder;
        this.errorReceiverProxy = errorReceiverProxy;
        this.errorWriterProxy = errorWriterProxy;
        this.recordErrorReceiver = recordErrorReceiver;
        this.pipelineDataCache = pipelineDataCache;
        this.streamHolder = streamHolder;
        this.security = security;
    }

    @Override
    public VoidResult exec(final CliTranslationTask task) {
        return security.secureResult(() -> {
            try {
                final ErrorWriter errorWriter = new CliErrorWriter(task.getErrorWriter());

                // Setup the error handler and receiver.
                errorWriterProxy.setErrorWriter(errorWriter);
                errorReceiverProxy.setErrorReceiver(recordErrorReceiver);

                final InputStream dataStream = task.getDataStream();
                final InputStream metaStream = task.getMetaStream();

                if (metaStream == null) {
                    throw new RuntimeException("No meta data found");
                }

                // Load the meta and context data.
                final AttributeMap metaData = new AttributeMap();
                AttributeMapUtil.read(metaStream, false, metaData);

                // Get the feed.
                final String feedName = metaData.get(StroomHeaderArguments.FEED);
                final FeedDoc feed = getFeed(feedName);
                feedHolder.setFeedName(feedName);

                // Setup the meta data holder.
                metaDataHolder.setMetaDataProvider(new StreamMetaDataProvider(streamHolder, pipelineStore));

                // Set the pipeline so it can be used by a filter if needed.
                final List<DocRef> pipelines = pipelineStore.findByName(feedName);
                if (pipelines == null || pipelines.size() == 0) {
                    throw new ProcessException("No pipeline found for feed name '" + feedName + "'");
                }
                if (pipelines.size() > 1) {
                    throw new ProcessException("More than one pipeline found for feed name '" + feedName + "'");
                }

                final DocRef pipelineRef = pipelines.get(0);
                pipelineHolder.setPipeline(pipelineRef);

                // Create the parser.
                final PipelineDoc pipelineDoc = pipelineStore.readDocument(pipelineRef);
                final PipelineData pipelineData = pipelineDataCache.get(pipelineDoc);
                final Pipeline pipeline = pipelineFactory.create(pipelineData);

                // Output the meta data for the new stream.
                this.metaData.putAll(metaData);

                // Set effective time.
                Long effectiveMs = null;
                try {
                    final String effectiveTime = metaData.get(StroomHeaderArguments.EFFECTIVE_TIME);
                    if (effectiveTime != null && !effectiveTime.isEmpty()) {
                        effectiveMs = DateUtil.parseNormalDateTimeString(effectiveTime);
                    }
                } catch (final RuntimeException e) {
                    outputError(e);
                }

                // Create the stream.
                final Data stream = new DataImpl.Builder()
                        .effectiveMs(effectiveMs)
                        .feedName(feedName)
                        .build();

                // Add stream providers for lookups etc.
                final BasicInputStreamProvider streamProvider = new BasicInputStreamProvider(
                        new IgnoreCloseInputStream(task.getDataStream()), task.getDataStream().available());
                streamHolder.setStream(stream);
                streamHolder.addProvider(streamProvider, StreamTypeNames.RAW_EVENTS);
                if (task.getMetaStream() != null) {
                    final BasicInputStreamProvider metaStreamProvider = new BasicInputStreamProvider(
                            new IgnoreCloseInputStream(task.getMetaStream()), task.getMetaStream().available());
                    streamHolder.addProvider(metaStreamProvider, StreamTypeNames.META);
                }
                if (task.getContextStream() != null) {
                    final BasicInputStreamProvider contextStreamProvider = new BasicInputStreamProvider(
                            new IgnoreCloseInputStream(task.getContextStream()), task.getContextStream().available());
                    streamHolder.addProvider(contextStreamProvider, StreamTypeNames.CONTEXT);
                }

                try {
                    pipeline.process(dataStream, feed.getEncoding());
                } catch (final RuntimeException e) {
                    outputError(e);
                }
            } catch (final IOException | RuntimeException e) {
                outputError(e);
            }

            return VoidResult.INSTANCE;
        });
    }

    private FeedDoc getFeed(final String feedName) {
        if (feedName == null) {
            throw new RuntimeException("No feed name found in meta data");
        }

        final List<DocRef> docRefs = feedStore.findByName(feedName);
        if (docRefs.size() == 0) {
            throw new RuntimeException("No configuration found for feed \"" + feedName + "\"");
        }

        return feedStore.readDocument(docRefs.get(0));
    }

    /**
     * Used to handle any errors that may occur during translation.
     */
    private void outputError(final Throwable ex) {
        if (errorReceiverProxy != null && !(ex instanceof LoggedException)) {
            try {
                if (ex.getMessage() != null) {
                    errorReceiverProxy.log(Severity.FATAL_ERROR, null, "PipelineStreamProcessor", ex.getMessage(), ex);
                } else {
                    errorReceiverProxy.log(Severity.FATAL_ERROR, null, "PipelineStreamProcessor", ex.toString(), ex);
                }
            } catch (final RuntimeException e) {
                // Ignore exception as we generated it.
            }

            if (errorReceiverProxy.getErrorReceiver() instanceof ErrorStatistics) {
                ((ErrorStatistics) errorReceiverProxy.getErrorReceiver()).checkRecord(-1);
            }
        }
    }
}
