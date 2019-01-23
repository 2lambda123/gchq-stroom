/*
 * Copyright 2016 Crown Copyright
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
 */

package stroom.pipeline.task;


import org.junit.jupiter.api.Test;
import stroom.data.meta.api.Data;
import stroom.data.meta.impl.mock.MockDataMetaService;
import stroom.data.store.impl.fs.MockStreamStore;
import stroom.node.NodeInfo;
import stroom.streamstore.shared.StreamTypeNames;
import stroom.streamtask.StreamProcessorTask;
import stroom.streamtask.StreamProcessorTaskExecutor;
import stroom.streamtask.StreamTaskCreator;
import stroom.streamtask.shared.ProcessorFilterTask;
import stroom.task.api.TaskManager;
import stroom.test.AbstractProcessIntegrationTest;
import stroom.test.ComparisonHelper;
import stroom.test.StoreCreationTool;
import stroom.test.StroomPipelineTestFileUtil;
import stroom.util.shared.Severity;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import static org.assertj.core.api.Assertions.assertThat;

class TestTranslationTaskWithoutTranslation extends AbstractProcessIntegrationTest {
    private static final String DIR = "TestTranslationTaskWithoutTranslation/";
    private static final String FEED_NAME = "TEST_FEED";
    private static final Path RESOURCE_NAME = StroomPipelineTestFileUtil.getTestResourcesFile(DIR + "TestTask.out");

    @Inject
    private MockStreamStore streamStore;
    @Inject
    private MockDataMetaService streamMetaService;
    @Inject
    private NodeInfo nodeInfo;
    @Inject
    private StreamTaskCreator streamTaskCreator;
    @Inject
    private StoreCreationTool storeCreationTool;
    @Inject
    private TaskManager taskManager;

    /**
     * Tests Task with a valid resource and feed.
     *
     * @throws IOException Could be thrown.
     */
    @Test
    void test() throws IOException {
        setup(FEED_NAME, RESOURCE_NAME);
        assertThat(streamMetaService.getLockCount()).isEqualTo(0);

        final List<StreamProcessorTaskExecutor> results = processAll();
        assertThat(results.size()).isEqualTo(1);

        for (final StreamProcessorTaskExecutor result : results) {
            final PipelineStreamProcessor processor = (PipelineStreamProcessor) result;
            final String message = "Count = " + processor.getRead() + "," + processor.getWritten() + ","
                    + processor.getMarkerCount(Severity.SEVERITIES);

            assertThat(processor.getWritten() > 0).as(message).isTrue();
            assertThat(processor.getRead() <= processor.getWritten()).as(message).isTrue();
            assertThat(processor.getMarkerCount(Severity.SEVERITIES)).as(message).isEqualTo(0);
        }

        final Path inputDir = StroomPipelineTestFileUtil.getTestResourcesDir().resolve(DIR);
        final Path outputDir = StroomPipelineTestFileUtil.getTestOutputDir().resolve(DIR);

        for (final Entry<Long, Data> entry : streamMetaService.getDataMap().entrySet()) {
            final long streamId = entry.getKey();
            final Data stream = entry.getValue();
            if (StreamTypeNames.EVENTS.equals(stream.getTypeName())) {
                final byte[] data = streamStore.getFileData().get(streamId).get(stream.getTypeName());

                // Write the actual XML out.
                final OutputStream os = StroomPipelineTestFileUtil.getOutputStream(outputDir, "TestTask.out");
                os.write(data);
                os.flush();
                os.close();

                ComparisonHelper.compareDirs(inputDir, outputDir);
            }
        }

        // Make sure 10 records were written.
        assertThat(((PipelineStreamProcessor) results.get(0)).getWritten()).isEqualTo(10);
    }

    /**
     * Gets the next task to be processed.
     *
     * @return The next task or null if there are currently no more tasks.
     */
    private List<StreamProcessorTaskExecutor> processAll() {
        final List<StreamProcessorTaskExecutor> results = new ArrayList<>();
        List<ProcessorFilterTask> streamTasks = streamTaskCreator.assignStreamTasks(nodeInfo.getThisNodeName(), 100);
        while (streamTasks.size() > 0) {
            for (final ProcessorFilterTask streamTask : streamTasks) {
                final StreamProcessorTask task = new StreamProcessorTask(streamTask);
                taskManager.exec(task);
                results.add(task.getStreamProcessorTaskExecutor());
            }
            streamTasks = streamTaskCreator.assignStreamTasks(nodeInfo.getThisNodeName(), 100);
        }
        return results;
    }

    private void setup(final String feedName, final Path dataLocation) throws IOException {
        storeCreationTool.addEventData(feedName, null, null, null, dataLocation, null);
    }
}
