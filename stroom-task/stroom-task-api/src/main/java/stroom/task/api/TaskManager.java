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

package stroom.task.api;

import stroom.task.shared.FindTaskCriteria;
import stroom.task.shared.FindTaskProgressCriteria;
import stroom.task.shared.Task;
import stroom.task.shared.TaskId;
import stroom.task.shared.TaskProgress;
import stroom.task.shared.ThreadPool;
import stroom.util.entity.FindService;
import stroom.util.shared.ResultPage;

public interface TaskManager extends FindService<TaskProgress, FindTaskProgressCriteria> {
    void startup();

    void shutdown();

    /**
     * Execute a task synchronously.
     *
     * @param task The task to execute.
     * @return The result of the task execution.
     */
    <R> R exec(Task<R> task);

    /**
     * Execute a task asynchronously without expecting to handle any result via
     * a callback.
     *
     * @param task The task to execute asynchronously.
     */
    <R> void execAsync(Task<R> task);

    /**
     * Execute a task asynchronously with a callback to receive results.
     *
     * @param task     The task to execute asynchronously.
     * @param callback The callback that will receive results from the task
     *                 execution.
     */
    <R> void execAsync(Task<R> task, TaskCallback<R> callback);

    /**
     * Execute a task asynchronously without expecting to handle any result via
     * a callback.
     *
     * @param task       The task to execute asynchronously.
     * @param threadPool The thread pool to use for execution.
     */
    <R> void execAsync(Task<R> task, ThreadPool threadPool);

    /**
     * Execute a task asynchronously with a callback to receive results.
     *
     * @param task       The task to execute asynchronously.
     * @param callback   The callback that will receive results from the task
     *                   execution.
     * @param threadPool The thread pool to use for execution.
     */
    <R> void execAsync(Task<R> task, TaskCallback<R> callback, ThreadPool threadPool);

    void execAsync(Task<?> parentTask, String taskName, Runnable runnable, ThreadPool threadPool);

    /**
     * Get a currently executing task by id.
     */
    Task<?> getTaskById(TaskId taskId);

    /**
     * Find out if the task with the given id is terminated.
     */
    boolean isTerminated(TaskId taskId);

    /**
     * Terminate a task by task id.
     *
     * @param taskId The id of the task to terminate.
     */
    void terminate(TaskId taskId);

    ResultPage<TaskProgress> terminate(FindTaskCriteria criteria, boolean kill);

    int getCurrentTaskCount();
}
