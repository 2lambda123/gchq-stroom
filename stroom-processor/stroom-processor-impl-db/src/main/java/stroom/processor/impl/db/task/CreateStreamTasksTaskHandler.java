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

package stroom.processor.impl.db.task;

import stroom.processor.impl.db.CreateStreamTasksTask;
import stroom.processor.impl.db.StreamTaskCreator;
import stroom.security.Security;
import stroom.task.api.AbstractTaskHandler;
import stroom.task.api.TaskContext;
import stroom.util.shared.VoidResult;

import javax.inject.Inject;


public class CreateStreamTasksTaskHandler extends AbstractTaskHandler<CreateStreamTasksTask, VoidResult> {
    private final StreamTaskCreator streamTaskCreator;
    private final TaskContext taskContext;
    private final Security security;

    @Inject
    CreateStreamTasksTaskHandler(final StreamTaskCreator streamTaskCreator,
                                 final TaskContext taskContext,
                                 final Security security) {
        this.streamTaskCreator = streamTaskCreator;
        this.taskContext = taskContext;
        this.security = security;
    }

    @Override
    public VoidResult exec(final CreateStreamTasksTask task) {
        return security.secureResult(() -> {
            streamTaskCreator.createTasks(taskContext);
            return new VoidResult();
        });
    }
}
