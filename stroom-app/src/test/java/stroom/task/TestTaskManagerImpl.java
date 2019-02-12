package stroom.task;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.task.api.ExecutorProvider;
import stroom.task.impl.ThreadPoolImpl;
import stroom.task.shared.ThreadPool;
import stroom.test.AbstractCoreIntegrationTest;

import javax.inject.Inject;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TestTaskManagerImpl extends AbstractCoreIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestTaskManagerImpl.class);

    @Inject
    ExecutorProvider executorProvider;

    @Test
    void testMoreItemsThanThreads_boundedPool() throws ExecutionException, InterruptedException {
        LOGGER.info("Starting");
        int poolSize = 4;
        ThreadPool threadPool = new ThreadPoolImpl(getClass().getName(),
                3,
                poolSize,
                poolSize);

        final Executor executor = executorProvider.getExecutor(threadPool);


        CompletableFuture.runAsync(() ->
                LOGGER.info("Warming up thread pool")).get();


        AtomicInteger counter = new AtomicInteger();
        final Queue<Thread> threadsUsed = new ConcurrentLinkedQueue<>();

        final int taskCount = 10;
        CompletableFuture[] futures = IntStream.rangeClosed(1, taskCount)
                .mapToObj(i ->
                        CompletableFuture.runAsync(() -> {
                                    try {
                                        Thread.sleep(50);
                                        LOGGER.info("Running task %s", i);
                                        counter.incrementAndGet();
                                        threadsUsed.add(Thread.currentThread());
                                    } catch (final InterruptedException e) {
                                        LOGGER.error(e.getMessage(), e);

                                        // Continue to interrupt this thread.
                                        Thread.currentThread().interrupt();
                                    }
                                },
                                executor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).get();

        assertThat(counter.get()).isEqualTo(taskCount);

        long distinctThreads = threadsUsed.stream()
                .distinct()
                .count();

        LOGGER.info("Threads used: %s", distinctThreads);

        assertThat(distinctThreads <= poolSize).isTrue();

        LOGGER.info("Finished");
    }

    @Test
    void testMoreItemsThanThreads_unBoundedPool() throws ExecutionException, InterruptedException {

        LOGGER.info("Starting");
        int poolSize = Integer.MAX_VALUE; //unbounded
        ThreadPool threadPool = new ThreadPoolImpl(this.getClass().getName(),
                3,
                0,
                poolSize);

        final Executor executor = executorProvider.getExecutor(threadPool);

        AtomicInteger counter = new AtomicInteger();
        final Queue<Thread> threadsUsed = new ConcurrentLinkedQueue<>();

        final int taskCount = 10;
        CompletableFuture[] futures = IntStream.rangeClosed(1, taskCount)
                .mapToObj(i ->
                        CompletableFuture.runAsync(() -> {
                                    try {
                                        Thread.sleep(50);
                                        LOGGER.info("Running task %s", i);
                                        counter.incrementAndGet();
                                        threadsUsed.add(Thread.currentThread());
                                    } catch (final InterruptedException e) {
                                        LOGGER.error(e.getMessage(), e);

                                        // Continue to interrupt this thread.
                                        Thread.currentThread().interrupt();
                                    }
                                },
                                executor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).get();

        assertThat(counter.get()).isEqualTo(taskCount);

        long distinctThreads = threadsUsed.stream()
                .distinct()
                .count();

        LOGGER.info("Threads used: %s", distinctThreads);

        assertThat(distinctThreads <= poolSize).isTrue();
        LOGGER.info("Finished");
    }

}
