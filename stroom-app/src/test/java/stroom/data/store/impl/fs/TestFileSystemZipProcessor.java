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

package stroom.data.store.impl.fs;


import org.junit.jupiter.api.Test;
import stroom.meta.shared.AttributeMap;
import stroom.data.store.api.NestedInputStream;
import stroom.data.store.api.StreamSource;
import stroom.data.store.api.StreamStore;
import stroom.pipeline.feed.FeedDocCache;
import stroom.meta.shared.StroomHeaderArguments;
import stroom.proxy.repo.StroomStreamProcessor;
import stroom.streamstore.shared.StreamTypeNames;
import stroom.streamtask.StreamTargetStroomStreamHandler;
import stroom.test.AbstractCoreIntegrationTest;
import stroom.util.io.StreamUtil;
import stroom.util.test.FileSystemTestUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class TestFileSystemZipProcessor extends AbstractCoreIntegrationTest {
    @Inject
    private StreamStore streamStore;
    @Inject
    private FeedDocCache feedDocCache;
    @Inject
    private FileSystemStreamMaintenanceService streamMaintenanceService;

    @Test
    void testSimpleSingleFile() throws IOException {
        final Path file = getCurrentTestDir().resolve(
                FileSystemTestUtil.getUniqueTestString() + "TestFileSystemZipProcessor.zip");
        try {
            final ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(file));
            zipOut.putNextEntry(new ZipEntry("tom1.dat"));
            zipOut.write("File1\nFile1\n".getBytes());
            zipOut.closeEntry();
            zipOut.close();

            final HashMap<String, String> expectedContent = new HashMap<>();
            expectedContent.put(null, "File1\nFile1\n");
            final HashMap<String, List<String>> expectedBoundaries = new HashMap<>();
            expectedBoundaries.put(null, Collections.singletonList("File1\nFile1\n"));

            doTest(file, 1, new HashSet<>(Arrays.asList("revt.bgz", "revt.meta.bgz", "revt.mf.dat")),
                    expectedContent, expectedBoundaries);
        } finally {
            Files.delete(file);
        }
    }

    @Test
    void testSimpleSingleFileReadThreeTimes() throws IOException {
        final Path file = getCurrentTestDir().resolve(
                FileSystemTestUtil.getUniqueTestString() + "TestFileSystemZipProcessor.zip");
        try {
            final ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(file));
            zipOut.putNextEntry(new ZipEntry("tom1.dat"));
            zipOut.write("File1\nFile1\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.close();

            final HashMap<String, String> expectedContent = new HashMap<>();
            expectedContent.put(null, "File1\nFile1\nFile1\nFile1\nFile1\nFile1\n");
            final HashMap<String, List<String>> expectedBoundaries = new HashMap<>();
            expectedBoundaries.put(null, Arrays.asList("File1\nFile1\n", "File1\nFile1\n", "File1\nFile1\n"));

            doTest(file, 3, new HashSet<>(
                            Arrays.asList("revt.bgz", "revt.bdy.dat", "revt.meta.bgz", "revt.meta.bdy.dat", "revt.mf.dat")),
                    expectedContent, expectedBoundaries);
        } finally {
            Files.delete(file);
        }
    }

    @Test
    void testSimpleSingleFileWithMetaAndContext() throws IOException {
        final Path file = getCurrentTestDir().resolve(
                FileSystemTestUtil.getUniqueTestString() + "TestFileSystemZipProcessor.zip");
        try {
            final ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(file));
            zipOut.putNextEntry(new ZipEntry("tom1.dat"));
            zipOut.write("File1\nFile1\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("tom1.ctx"));
            zipOut.write("Context1\nContext1\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("tom1.meta"));
            zipOut.write("Meta11:1\nMeta12:1\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.close();

            final HashMap<String, String> expectedContent = new HashMap<>();
            expectedContent.put(null, "File1\nFile1\n");
            expectedContent.put(StreamTypeNames.CONTEXT, "Context1\nContext1\n");
            expectedContent.put(StreamTypeNames.META, "Meta11:1\nMeta12:1\nStreamSize:12\n");

            final HashMap<String, List<String>> expectedBoundaries = new HashMap<>();
            expectedBoundaries.put(null, Collections.singletonList("File1\nFile1\n"));
            expectedBoundaries.put(StreamTypeNames.CONTEXT, Collections.singletonList("Context1\nContext1\n"));
            expectedBoundaries.put(StreamTypeNames.META, Collections.singletonList("Meta11:1\nMeta12:1\nStreamSize:12\n"));

            doTest(file, 1,
                    new HashSet<>(Arrays.asList("revt.bgz", "revt.ctx.bgz", "revt.meta.bgz", "revt.mf.dat")),
                    expectedContent, expectedBoundaries);
        } finally {
            Files.delete(file);
        }

    }

    @Test
    void testMultiFileWithMetaAndContext() throws IOException {
        final Path file = getCurrentTestDir().resolve(
                FileSystemTestUtil.getUniqueTestString() + "TestFileSystemZipProcessor.zip");
        try {
            // Build a zip with an odd order
            final ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(file));
            zipOut.putNextEntry(new ZipEntry("tom1.dat"));
            zipOut.write("File1\nFile1\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("tom2.dat"));
            zipOut.write("File2\nFile2\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("tom1.ctx"));
            zipOut.write("Context1\nContext1\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("tom1.meta"));
            zipOut.write("Meta1a\nMeta1b\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("tom2.meta"));
            zipOut.write("Meta2a\nMeta2b\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("tom2.ctx"));
            zipOut.write("Context2\nContext2\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.close();

            final HashMap<String, String> expectedContent = new HashMap<>();
            expectedContent.put(null, "File1\nFile1\nFile2\nFile2\n");
            expectedContent.put(StreamTypeNames.CONTEXT, "Context1\nContext1\nContext2\nContext2\n");
            expectedContent.put(StreamTypeNames.META, "Meta1a\nMeta1b\nStreamSize:12\nMeta2a\nMeta2b\nStreamSize:12\n");

            final HashMap<String, List<String>> expectedBoundaries = new HashMap<>();
            expectedBoundaries.put(null, Arrays.asList("File1\nFile1\n", "File2\nFile2\n"));
            expectedBoundaries.put(StreamTypeNames.CONTEXT, Arrays.asList("Context1\nContext1\n", "Context2\nContext2\n"));
            expectedBoundaries.put(StreamTypeNames.META,
                    Arrays.asList("Meta1a\nMeta1b\nStreamSize:12\n", "Meta2a\nMeta2b\nStreamSize:12\n"));

            doTest(file, 1, new HashSet<>(Arrays.asList("revt.bgz", "revt.bdy.dat", "revt.ctx.bgz",
                    "revt.ctx.bdy.dat", "revt.meta.bgz", "revt.meta.bdy.dat", "revt.mf.dat")), expectedContent,
                    expectedBoundaries);
        } finally {
            Files.delete(file);
        }
    }

    @Test
    void testMultiFile() throws IOException {
        final Path file = getCurrentTestDir().resolve(
                FileSystemTestUtil.getUniqueTestString() + "TestFileSystemZipProcessor.zip");
        try {
            final ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(file));
            zipOut.putNextEntry(new ZipEntry("tom1.dat"));
            zipOut.write("File1\nFile1\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("tom2.dat"));
            zipOut.write("File2\nFile2\n".getBytes(StreamUtil.DEFAULT_CHARSET));
            zipOut.closeEntry();
            zipOut.close();

            final HashMap<String, String> expectedContent = new HashMap<>();
            expectedContent.put(null, "File1\nFile1\nFile2\nFile2\n");
            final HashMap<String, List<String>> expectedBoundaries = new HashMap<>();
            expectedBoundaries.put(null, Arrays.asList("File1\nFile1\n", "File2\nFile2\n"));

            doTest(file, 1, new HashSet<>(
                            Arrays.asList("revt.bgz", "revt.bdy.dat", "revt.meta.bgz", "revt.meta.bdy.dat", "revt.mf.dat")),
                    expectedContent, expectedBoundaries);
        } finally {
            Files.delete(file);
        }
    }

    private void doTest(final Path file, final int processCount, final Set<String> expectedFiles,
                        final HashMap<String, String> expectedContent,
                        final HashMap<String, List<String>> expectedBoundaries) throws IOException {
        final String feedName = FileSystemTestUtil.getUniqueTestString();

        final AttributeMap attributeMap = new AttributeMap();
        attributeMap.put(StroomHeaderArguments.COMPRESSION, StroomHeaderArguments.COMPRESSION_ZIP);

        final List<StreamTargetStroomStreamHandler> handlerList = StreamTargetStroomStreamHandler
                .buildSingleHandlerList(streamStore, feedDocCache, null, feedName, StreamTypeNames.RAW_EVENTS);

        final StroomStreamProcessor stroomStreamProcessor = new StroomStreamProcessor(attributeMap, handlerList, new byte[1000],
                "DefaultDataFeedRequest-" + attributeMap.get(StroomHeaderArguments.GUID));
        stroomStreamProcessor.setAppendReceivedPath(false);

        for (int i = 0; i < processCount; i++) {
            stroomStreamProcessor.process(Files.newInputStream(file), String.valueOf(i));
        }

        stroomStreamProcessor.closeHandlers();

        final List<Path> files = streamMaintenanceService
                .findAllStreamFile(handlerList.get(0).getStreamSet().iterator().next());

        final HashSet<String> foundFiles = new HashSet<>();

        for (final Path rfile : files) {
            if (Files.isRegularFile(rfile)) {
                String fileName = rfile.getFileName().toString();
                fileName = fileName.substring(fileName.indexOf(".") + 1);
                foundFiles.add(fileName);
            }
        }
        assertThat(foundFiles).as("Checking expected output files").isEqualTo(expectedFiles);

        // Test full content
        StreamSource source = streamStore.openStreamSource(handlerList.get(0).getStreamSet().iterator().next().getId());
        for (final Entry<String, String> entry : expectedContent.entrySet()) {
            final String key = entry.getKey();
            final String content = entry.getValue();
            if (key == null) {
                assertThat(StreamUtil.streamToString(source.getInputStream())).isEqualTo(content);
            } else {
                assertThat(StreamUtil.streamToString(source.getChildStream(key).getInputStream())).isEqualTo(content);
            }
        }
        streamStore.closeStreamSource(source);

        // Test boundaries
        source = streamStore.openStreamSource(handlerList.get(0).getStreamSet().iterator().next().getId());
        for (final Entry<String, List<String>> entry : expectedBoundaries.entrySet()) {
            final String key = entry.getKey();
            final List<String> nestedContentList = entry.getValue();
            if (key == null) {
                final NestedInputStream inputStream = source.getNestedInputStream();
                for (final String nestedContent : nestedContentList) {
                    assertThat(inputStream.getNextEntry()).isTrue();
                    assertThat(StreamUtil.streamToString(inputStream, false)).isEqualTo(nestedContent);
                    inputStream.closeEntry();
                }
                inputStream.close();
            } else {
                final NestedInputStream inputStream = source.getChildStream(key).getNestedInputStream();
                for (final String nestedContent : nestedContentList) {
                    assertThat(inputStream.getNextEntry()).isTrue();
                    assertThat(StreamUtil.streamToString(inputStream, false)).isEqualTo(nestedContent);
                    inputStream.closeEntry();
                }
                inputStream.close();
            }
        }
        streamStore.closeStreamSource(source);
    }
}
