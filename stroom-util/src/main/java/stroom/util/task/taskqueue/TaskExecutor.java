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

package stroom.util.task.taskqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.util.spring.StroomShutdown;
import stroom.util.thread.CustomThreadFactory;
import stroom.util.thread.StroomThreadGroup;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TaskExecutor {
    private static final int DEFAULT_MAX_THREADS = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutor.class);

    private volatile int maxThreads = DEFAULT_MAX_THREADS;

    private final AtomicInteger totalThreads = new AtomicInteger();

    private final ConcurrentSkipListSet<TaskProducer> producers = new ConcurrentSkipListSet<>();
    private final AtomicReference<TaskProducer> lastProducer = new AtomicReference<>();

    private final ReentrantLock taskLock = new ReentrantLock();
    private final Condition condition = taskLock.newCondition();

    private final String name;
    private volatile ExecutorService executor;
    private volatile boolean running;
    private volatile boolean shutdown;

    public TaskExecutor(final String name) {
        this.name = name;
    }

    private synchronized void start() {
        if (!running && !shutdown) {
            running = true;

            final ThreadGroup poolThreadGroup = new ThreadGroup(StroomThreadGroup.instance(), name);
            final CustomThreadFactory threadFactory = new CustomThreadFactory(name, poolThreadGroup, 5);
            executor = Executors.newSingleThreadExecutor(threadFactory);
            executor.execute(() -> {
                while (running) {
                    taskLock.lock();
                    try {
                        Runnable task = execNextTask();
                        if (task == null) {
                            final boolean didWait = condition.await(10, TimeUnit.SECONDS);

                            // If we didn't get any new work to do after a minute then output some debug so we can check
                            // the task producer signalling code is working correctly.
                            if (LOGGER.isDebugEnabled()) {
                                try {
                                    if (!didWait) {
                                        if (getRemainingTaskCount() > 0) {
                                            LOGGER.debug(getDebugMessage());
                                        } else if (LOGGER.isTraceEnabled()) {
                                            LOGGER.trace(getDebugMessage());
                                        }
                                    }
                                } catch (final RuntimeException e) {
                                    LOGGER.trace(e.getMessage(), e);
                                }
                            }
                        }
                    } catch (final InterruptedException e) {
                        // Clear the interrupt state.
                        Thread.interrupted();
                    } catch (final RuntimeException e) {
                        LOGGER.error(e.getMessage(), e);
                    } finally {
                        taskLock.unlock();
                    }
                }
            });
        }
    }

    private String getDebugMessage() {
        final StringBuilder sb = new StringBuilder("Timeout waiting for:");

        for (final TaskProducer producer : producers) {
            if (producer != null) {
                sb.append("\n\t");
                sb.append(producer.getClass().getSimpleName());
                sb.append(" (remaining=");
                sb.append(producer.getRemainingTasks());
                sb.append(", completed=");
                sb.append(producer.getTasksCompleted());
                sb.append(", total=");
                sb.append(producer.getTasksTotal());
                sb.append(")");
            }
        }

        return sb.toString();
    }

    private int getRemainingTaskCount() {
        int count = 0;
        for (final TaskProducer producer : producers) {
            if (producer != null) {
                count += producer.getRemainingTasks();
            }
        }
        return count;
    }

    private synchronized void stop() {
        if (running) {
            running = false;
            // Wake up any waiting threads.
            signalAll();
            executor.shutdown();
        }
    }

    @StroomShutdown
    public void shutdown() {
        shutdown = true;
        stop();
    }

    final void addProducer(final TaskProducer producer) {
        if (!running) {
            start();
        }
        producers.add(producer);
    }

    final void removeProducer(final TaskProducer producer) {
        producers.remove(producer);
    }

    final void signalAll() {
        taskLock.lock();
        try {
            condition.signalAll();
        } finally {
            taskLock.unlock();
        }
    }

    private Runnable execNextTask() {
        TaskProducer producer = null;
        Runnable task = null;

        final int total = totalThreads.getAndIncrement();
        boolean executing = false;

        try {
            if (total < maxThreads) {
                // Try and get a task from usable producers.
                final int tries = producers.size();
                for (int i = 0; i < tries && task == null; i++) {
                    producer = nextProducer();
                    if (producer != null) {
                        task = producer.next();
                    }
                }

                final TaskProducer currentProducer = producer;
                final Runnable currentTask = task;

                if (currentTask != null) {
                    executing = true;
                    CompletableFuture.runAsync(currentTask, currentProducer.getExecutor())
                            .thenAccept(result -> complete())
                            .exceptionally(t -> {
                                complete();
                                LOGGER.error(t.getMessage(), t);
                                return null;
                            });
                }
            }
        } finally {
            if (!executing) {
                totalThreads.decrementAndGet();
            }
        }

        return task;
    }

    private void complete() {
        totalThreads.decrementAndGet();
        signalAll();
    }

    private TaskProducer nextProducer() {
        TaskProducer current;
        TaskProducer producer;

        do {
            current = lastProducer.get();
            producer = current;
            try {
                if (producer == null) {
                    producer = producers.first();
                } else {
                    producer = producers.higher(producer);
                    if (producer == null) {
                        producer = producers.first();
                    }
                }
            } catch (final Exception e) {
                // Ignore.
            }
        } while (!lastProducer.compareAndSet(current, producer));

        return producer;
    }

    public void setMaxThreads(final int maxThreads) {
        this.maxThreads = maxThreads;
    }
}
