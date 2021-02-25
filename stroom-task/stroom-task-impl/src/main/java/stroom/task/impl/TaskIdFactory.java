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

package stroom.task.impl;

import stroom.task.shared.TaskId;

import java.util.UUID;

class TaskIdFactory {

    static TaskId create() {
        return new TaskId(createUUID(), null);
    }

    static TaskId create(final TaskId parentTaskId) {
        if (parentTaskId != null) {
            return new TaskId(createUUID(), parentTaskId);
        }

        return create();
    }

    private static String createUUID() {
        return UUID.randomUUID().toString();
    }
}
