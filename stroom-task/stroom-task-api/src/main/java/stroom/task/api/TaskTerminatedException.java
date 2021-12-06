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

package stroom.task.api;

import stroom.util.shared.EntityServiceException;

public class TaskTerminatedException extends EntityServiceException {

    private final boolean stop;

    /**
     * Stop is true if the task manager is shutting down and no more tasks
     * should be submitted. This is useful in situations where tasks are queued
     * for execution and we want to know not to submit further tasks,
     */
    public TaskTerminatedException(final boolean stop) {
        super("Task terminated");
        this.stop = stop;
    }

    public TaskTerminatedException() {
        this(false);
    }

    /**
     * Stop is true if the task manager is shutting down and no more tasks
     * should be submitted. This is useful in situations where tasks are queued
     * for execution and we want to know not to submit further tasks,
     *
     * @return True if the task manager is shutting down.
     */
    public boolean isStop() {
        return stop;
    }
}
