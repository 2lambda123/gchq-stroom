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

import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.Clearable;
import stroom.node.NodeCache;
import stroom.node.shared.Node;
import stroom.data.meta.api.FindDataCriteria;
import stroom.data.meta.api.Data;
import stroom.data.meta.api.DataMetaService;
import stroom.streamstore.shared.QueryData;
import stroom.streamtask.shared.FindStreamProcessorFilterCriteria;
import stroom.streamtask.shared.ProcessorFilter;
import stroom.streamtask.shared.ProcessorFilterTask;
import stroom.streamtask.shared.TaskStatus;
import stroom.task.api.TaskContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Singleton
public class MockStreamTaskCreator implements StreamTaskCreator, Clearable {
    private final DataMetaService streamMetaService;
    private final StreamProcessorFilterService streamProcessorFilterService;
    private final NodeCache nodeCache;

    @Inject
    MockStreamTaskCreator(final DataMetaService streamMetaService,
                          final StreamProcessorFilterService streamProcessorFilterService,
                          final NodeCache nodeCache) {
        this.streamMetaService = streamMetaService;
        this.streamProcessorFilterService = streamProcessorFilterService;
        this.nodeCache = nodeCache;
    }

    @Override
    public void clear() {
        // NA
    }

    @Override
    public List<ProcessorFilterTask> assignStreamTasks(final String nodeName, final int count) {
        final Node node = nodeCache.getNode(nodeName);

        List<ProcessorFilterTask> taskList = Collections.emptyList();
        final FindStreamProcessorFilterCriteria criteria = new FindStreamProcessorFilterCriteria();
        final BaseResultList<ProcessorFilter> streamProcessorFilters = streamProcessorFilterService
                .find(criteria);
        if (streamProcessorFilters != null && streamProcessorFilters.size() > 0) {
            // Sort by priority.
            streamProcessorFilters.sort((o1, o2) -> o2.getPriority() - o1.getPriority());

            // Get tasks for each filter.
            taskList = new ArrayList<>();
            for (final ProcessorFilter filter : streamProcessorFilters) {
                final QueryData queryData = filter.getQueryData();

                final FindDataCriteria findStreamCriteria = new FindDataCriteria();
                findStreamCriteria.setExpression(queryData.getExpression());
                final BaseResultList<Data> streams = streamMetaService.find(findStreamCriteria);

                streams.sort(Comparator.comparing(Data::getId));

                if (streams.size() > 0) {
                    for (final Data stream : streams) {
                        if (stream.getId() >= filter.getStreamProcessorFilterTracker().getMinStreamId()) {
                            // Only process streams with an id of 1 or more
                            // greater than this stream in future.
                            filter.getStreamProcessorFilterTracker().setMinStreamId(stream.getId() + 1);

                            final ProcessorFilterTask streamTask = new ProcessorFilterTask();
                            streamTask.setStreamId(stream.getId());
                            streamTask.setStreamProcessorFilter(filter);
                            streamTask.setNode(node);
                            streamTask.setStatus(TaskStatus.ASSIGNED);

                            taskList.add(streamTask);
                        }
                    }
                }
            }
        }

        return taskList;
    }

    @Override
    public void createTasks(TaskContext taskContext) {
    }

    @Override
    public void startup() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void writeQueueStatistics() {

    }

    @Override
    public int getStreamTaskQueueSize() {
        return 0;
    }

    @Override
    public void abandonStreamTasks(final String nodeName, final List<ProcessorFilterTask> tasks) {
        // NA
    }
}
