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
 */

package stroom.task.impl;

import stroom.task.api.TaskContext;

class TaskContextImpl implements TaskContext {
    @Override
    public void setName(final String name) {
        CurrentTaskState.setName(name);
    }

    @Override
    public void info(final Object... args) {
        CurrentTaskState.info(args);
    }

    @Override
    public void terminate() {
        CurrentTaskState.terminate();
    }
}
