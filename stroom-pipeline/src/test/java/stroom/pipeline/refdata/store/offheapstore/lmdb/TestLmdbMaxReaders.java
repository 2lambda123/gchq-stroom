package stroom.pipeline.refdata.store.offheapstore.lmdb;

import stroom.bytebuffer.ByteBufferPoolFactory;
import stroom.lmdb.BasicLmdbDb;
import stroom.pipeline.refdata.store.offheapstore.serdes.StringSerde;
import stroom.util.io.ByteSize;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lmdbjava.Env;
import org.lmdbjava.EnvFlags;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class TestLmdbMaxReaders {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestLmdbMaxReaders.class);

    private static final ByteSize DB_MAX_SIZE = ByteSize.ofMebibytes(2_000);

    @Test
    void test() throws IOException, InterruptedException {

//        ThreadUtil.sleepAtLeastIgnoreInterrupts(10_000);

        final int maxReaders = 8;

        final Path dbDir = Files.createTempDirectory("stroom");

        final EnvFlags[] envFlags = new EnvFlags[]{EnvFlags.MDB_NOTLS};
//        final EnvFlags[] envFlags = new EnvFlags[]{};
        LOGGER.info("Creating LMDB environment with maxSize: {}, dbDir {}, envFlags {}",
                DB_MAX_SIZE,
                dbDir.toAbsolutePath(),
                Arrays.toString(envFlags));

        final Env<ByteBuffer> env = Env.create()
                .setMapSize(DB_MAX_SIZE.getBytes())
                .setMaxDbs(1)
                .setMaxReaders(maxReaders)
                .open(dbDir.toFile(), envFlags);

        final BasicLmdbDb<String, String> database = new BasicLmdbDb<>(
                env,
                new ByteBufferPoolFactory().getByteBufferPool(),
                new StringSerde(),
                new StringSerde(),
                "MyBasicLmdb");

        Assertions.assertThat(env.info().numReaders)
                .isEqualTo(0);

        // pad the keys to a fixed length so they sort in number order
        IntStream.rangeClosed(1, 5).forEach(i -> {
            database.put(buildKey(i), buildValue(i), false);
        });

        database.logDatabaseContents(LOGGER::info);

        Assertions.assertThat(env.info().numReaders)
                .isEqualTo(1);

        final int threadCount = 100;

        final CountDownLatch threadsFinishedLatch = new CountDownLatch(threadCount);

        final CountDownLatch threadsStartedLatch = new CountDownLatch(threadCount);
        final CountDownLatch releaseThreadsLatch = new CountDownLatch(1);

        final Executor executor = Executors.newFixedThreadPool(threadCount + 10);

        final Queue<String> threads = new ConcurrentLinkedQueue<>();

        final Semaphore semaphore = new Semaphore(maxReaders);

        IntStream.rangeClosed(1, threadCount)
                .forEach(i -> {
                    LOGGER.info("Creating thread {}", i);

                    CompletableFuture.runAsync(() -> {
                        LOGGER.info("Init thread {}", i);
                        threads.add(Thread.currentThread().getName());
                        threadsStartedLatch.countDown();
                        try {
                            // Get all threads to start work at the same time
                            releaseThreadsLatch.await();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        LOGGER.info("Running thread {}", i);

                        try {
                            // Use a semaphore to enforce the max concurrent read txns
                            // If we don't use the semaphore the test gets stuck, but not
                            // clear why, unless we set maxReaders > threadCount

                            semaphore.acquire();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        LOGGER.info("Got permit{}", i);

                        try (Txn<ByteBuffer> txnRead = env.txnRead()) {

                            Assertions.assertThat(database.get(txnRead, "01"))
                                    .isNotEmpty();
                        }
                        semaphore.release();
                        LOGGER.info("Finished thread {}", i);

                        threads.remove(Thread.currentThread().getName());
                        threadsFinishedLatch.countDown();
                    }, executor);
                });

        // wait for all threads to be ready to work
        threadsStartedLatch.await();

        // Release all threads at once to hit LMDB
        releaseThreadsLatch.countDown();

        do {
            LOGGER.info("numReaders: {}, threadsFinishedLatch: {}, threads: {}",
                    env.info().numReaders,
                    threadsFinishedLatch.getCount(),
                    String.join(", ", threads));

        } while (!threadsFinishedLatch.await(1, TimeUnit.SECONDS));

        // Wait for all threads to finish
        threadsFinishedLatch.await();

        LOGGER.info("numReaders: {}", env.info().numReaders);
    }

    private String buildKey(int i) {
        return String.format("%02d", i);
    }

    private String buildValue(int i) {
        return "value" + i;
    }
}
