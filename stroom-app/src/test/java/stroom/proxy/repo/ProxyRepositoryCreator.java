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

package stroom.proxy.repo;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.meta.shared.AttributeMap;
import stroom.meta.api.AttributeMapUtil;
import stroom.pipeline.feed.FeedDocCache;
import stroom.meta.shared.StandardHeaderArguments;
import stroom.feed.shared.FeedDoc;
import stroom.util.date.DateUtil;
import stroom.util.io.AbstractFileVisitor;
import stroom.util.io.FileUtil;
import stroom.util.io.StreamUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Optional;

public class ProxyRepositoryCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRepositoryCreator.class);

    private static final String INPUT_EXTENSION = ".in";
    private static final String ZIP_EXTENSION = ".zip";

    private final FeedDocCache feedDocCache;
    private final StroomZipRepository repository;

    public ProxyRepositoryCreator(final FeedDocCache feedDocCache, final StroomZipRepository repository) {
        this.feedDocCache = feedDocCache;
        this.repository = repository;
    }

    public void read(final Path dir, final boolean mandateEffectiveDate, final Long effectiveMs) {
        readDir(dir, mandateEffectiveDate, effectiveMs);
    }

    private void readDir(final Path dir, final boolean mandateEffectiveDate, final Long effectiveMs) {
        try {
            Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new AbstractFileVisitor() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    try {
                        final String fileName = file.getFileName().toString().toLowerCase();
                        if (fileName.endsWith(INPUT_EXTENSION)) {
                            loadInput(file, mandateEffectiveDate, effectiveMs);

                        } else if (fileName.endsWith(ZIP_EXTENSION)) {
                            loadZip(file, mandateEffectiveDate, effectiveMs);
                        }
                    } catch (final RuntimeException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void loadInput(final Path file, final boolean mandateEffectiveDate, final Long effectiveMs) {
        // Get the feed.
        final FeedDoc feed = getFeed(file);

        try {
            if (feed.isReference() == mandateEffectiveDate) {
                LOGGER.info("Loading data: " + FileUtil.getCanonicalPath(file));

                try (final StroomZipOutputStream zipOutputStream = repository.getStroomZipOutputStream()) {
                    int i = 0;
                    i++;
                    String newName = Integer.toString(i);
                    newName = Strings.padStart(newName, 3, '0');

                    // Add meta data.
                    final AttributeMap map = createMap(feed, effectiveMs);
                    try (final OutputStream zipPart = zipOutputStream.addEntry(new StroomZipEntry(null, newName, StroomZipFileType.Meta).getFullName())) {
                        AttributeMapUtil.write(map, zipPart);
                    }

                    // Add data.
                    try (final InputStream inputStream = new BufferedInputStream(Files.newInputStream(file));
                         final OutputStream zipPart = zipOutputStream.addEntry(new StroomZipEntry(null, newName, StroomZipFileType.Data).getFullName())) {
                        StreamUtil.streamToStream(inputStream, zipPart);
                    }
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void loadZip(final Path file, final boolean mandateEffectiveDate, final Long effectiveMs) {
        // Get the feed.
        final FeedDoc feed = getFeed(file);

        if (feed.isReference() == mandateEffectiveDate) {
            LOGGER.info("Loading data: " + FileUtil.getCanonicalPath(file));

            try {
                final StroomZipOutputStream zipOutputStream = repository.getStroomZipOutputStream();

                final StroomZipFile stroomZipFile = new StroomZipFile(file);

                int i = 0;
                for (String baseName : stroomZipFile.getStroomZipNameSet().getBaseNameSet()) {
                    i++;
                    String newName = Integer.toString(i);
                    newName = Strings.padStart(newName, 3, '0');

                    // Add meta data.
                    InputStream inputStream = stroomZipFile.getInputStream(baseName, StroomZipFileType.Meta);
                    final AttributeMap map = createMap(feed, effectiveMs);
                    if (inputStream != null) {
                        AttributeMapUtil.read(inputStream, true, map);
                    }
                    try (final OutputStream outputStream = zipOutputStream
                            .addEntry(new StroomZipEntry(null, newName, StroomZipFileType.Meta).getFullName())) {
                        AttributeMapUtil.write(map, outputStream);
                    }

                    // Add context data.
                    inputStream = stroomZipFile.getInputStream(baseName, StroomZipFileType.Context);
                    if (inputStream != null) {
                        try (final OutputStream outputStream = zipOutputStream
                                .addEntry(new StroomZipEntry(null, newName, StroomZipFileType.Context).getFullName())) {
                            StreamUtil.streamToStream(inputStream, outputStream);
                        }
                    }

                    // Add data.
                    inputStream = stroomZipFile.getInputStream(baseName, StroomZipFileType.Data);
                    if (inputStream != null) {
                        try (final OutputStream outputStream = zipOutputStream
                                .addEntry(new StroomZipEntry(null, newName, StroomZipFileType.Data).getFullName())) {
                            StreamUtil.streamToStream(inputStream, outputStream);
                        }
                    }
                }

                stroomZipFile.close();
                zipOutputStream.close();

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private AttributeMap createMap(final FeedDoc feed, final Long effectiveMs) {
        final String dateTime = DateUtil.createNormalDateTimeString(effectiveMs);

        final AttributeMap map = new AttributeMap();
        map.put(StandardHeaderArguments.FEED, feed.getName());
        map.put(StandardHeaderArguments.RECEIVED_TIME, dateTime);
        map.put(StandardHeaderArguments.EFFECTIVE_TIME, dateTime);
        map.put("TestData", "Loaded By SetupSampleData");

        return map;
    }

    private FeedDoc getFeed(final Path file) {
        // Get the stem of the file name.
        String stem = file.getFileName().toString();
        int index = stem.indexOf('.');
        if (index != -1) {
            stem = stem.substring(0, index);
        }

        // Find the associated feed.
        final Optional<FeedDoc> optional = feedDocCache.get(stem);
        if (!optional.isPresent()) {
            throw new RuntimeException("Feed not found \"" + stem + "\"");
        }

        return optional.get();
    }
}
