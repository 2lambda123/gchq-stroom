/*
 * Copyright 2018 Crown Copyright
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

package stroom.pipeline.refdata.store.offheapstore.lmdb;

import stroom.bytebuffer.ByteBufferPool;
import stroom.bytebuffer.ByteBufferPoolFactory;
import stroom.bytebuffer.ByteBufferUtils;
import stroom.bytebuffer.PooledByteBuffer;
import stroom.lmdb.BasicLmdbDb;
import stroom.lmdb.LmdbUtils;
import stroom.lmdb.PutOutcome;
import stroom.pipeline.refdata.store.offheapstore.databases.AbstractLmdbDbTest;
import stroom.pipeline.refdata.store.offheapstore.serdes.IntegerSerde;
import stroom.pipeline.refdata.store.offheapstore.serdes.StringSerde;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.KeyRange;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TestBasicLmdbDb extends AbstractLmdbDbTest {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(TestBasicLmdbDb.class);
    private final ByteBufferPool byteBufferPool = new ByteBufferPoolFactory().getByteBufferPool();

    private BasicLmdbDb<String, String> basicLmdbDb;
    private BasicLmdbDb<String, String> basicLmdbDb2;
    private BasicLmdbDb<Integer, String> basicLmdbDb3;
    private BasicLmdbDb<Integer, String> basicLmdbDb4;

    @BeforeEach
    void setup() {
        basicLmdbDb = new BasicLmdbDb<>(
                lmdbEnv,
                new ByteBufferPoolFactory().getByteBufferPool(),
                new StringSerde(),
                new StringSerde(),
                "MyBasicLmdb");

        basicLmdbDb2 = new BasicLmdbDb<>(
                lmdbEnv,
                new ByteBufferPoolFactory().getByteBufferPool(),
                new StringSerde(),
                new StringSerde(),
                "MyBasicLmdb2");

        basicLmdbDb3 = new BasicLmdbDb<>(
                lmdbEnv,
                new ByteBufferPoolFactory().getByteBufferPool(),
                new IntegerSerde(),
                new StringSerde(),
                "MyBasicLmdb3");

        basicLmdbDb4 = new BasicLmdbDb<>(
                lmdbEnv,
                new ByteBufferPoolFactory().getByteBufferPool(),
                new IntegerSerde(),
                new StringSerde(),
                "MyBasicLmdb4",
                DbiFlags.MDB_CREATE,
                DbiFlags.MDB_INTEGERKEY);
    }

    @Test
    void testPutDuplicate_noOverwrite() {
        byteBufferPool.doWithBufferPair(50, 50, (keyBuffer, valueBuffer) -> {
            basicLmdbDb.getKeySerde().serialize(keyBuffer, "MyKey");
            basicLmdbDb.getValueSerde().serialize(valueBuffer, "MyValue");

            LmdbUtils.doWithWriteTxn(lmdbEnv, writeTxn -> {
                PutOutcome putOutcome = basicLmdbDb.put(
                        writeTxn,
                        keyBuffer,
                        valueBuffer,
                        false);

                assertThat(putOutcome.isSuccess())
                        .isTrue();
                assertThat(putOutcome.isDuplicate())
                        .hasValue(false);

                putOutcome = basicLmdbDb.put(
                        writeTxn,
                        keyBuffer,
                        valueBuffer,
                        false);

                assertThat(putOutcome.isSuccess())
                        .isFalse();
                assertThat(putOutcome.isDuplicate())
                        .hasValue(true);
            });
            assertThat(basicLmdbDb.getEntryCount())
                    .isEqualTo(1);
        });
    }

    @Test
    void testPutDuplicate_overwrite() {
        byteBufferPool.doWithBufferPair(50, 50, (keyBuffer, valueBuffer) -> {
            basicLmdbDb.getKeySerde().serialize(keyBuffer, "MyKey");
            basicLmdbDb.getValueSerde().serialize(valueBuffer, "MyValue");

            LmdbUtils.doWithWriteTxn(lmdbEnv, writeTxn -> {
                PutOutcome putOutcome = basicLmdbDb.put(
                        writeTxn,
                        keyBuffer,
                        valueBuffer,
                        true);

                assertThat(putOutcome.isSuccess())
                        .isTrue();
                assertThat(putOutcome.isDuplicate())
                        .hasValue(false);

                putOutcome = basicLmdbDb.put(
                        writeTxn,
                        keyBuffer,
                        valueBuffer,
                        true);

                assertThat(putOutcome.isSuccess())
                        .isTrue();
                assertThat(putOutcome.isDuplicate())
                        .hasValue(true);
            });
            assertThat(basicLmdbDb.getEntryCount())
                    .isEqualTo(1);
        });
    }

    @Test
    void testBufferMutationAfterPut() {

        byteBufferPool.doWithBufferPair(50, 50, (keyBuffer, valueBuffer) -> {
            basicLmdbDb.getKeySerde().serialize(keyBuffer, "MyKey");
            basicLmdbDb.getValueSerde().serialize(valueBuffer, "MyValue");

            LmdbUtils.doWithWriteTxn(lmdbEnv, writeTxn -> {
                basicLmdbDb.put(writeTxn, keyBuffer, valueBuffer, false);
            });
            assertThat(basicLmdbDb.getEntryCount()).isEqualTo(1);

            // it is ok to mutate the buffers used in the put outside of the txn
            basicLmdbDb.getKeySerde().serialize(keyBuffer, "XX");
            basicLmdbDb.getValueSerde().serialize(valueBuffer, "YY");

            // now get the value again and it should be correct
            String val = basicLmdbDb.get("MyKey").get();

            assertThat(val).isEqualTo("MyValue");
        });
    }

    /**
     * This test is an example of how to abuse LMDB. If run it will crash the JVM
     * as a direct bytebuffer returned from a get() was mutated outside the transaction.
     */
    @Disabled // see javadoc above
    @Test
    void testBufferMutationAfterGet() {

        basicLmdbDb.put("MyKey", "MyValue", false);

        // the buffers have been returned to the pool and cleared
        assertThat(basicLmdbDb.getByteBufferPool().getCurrentPoolSize()).isEqualTo(2);

        assertThat(basicLmdbDb.getEntryCount()).isEqualTo(1);

        ByteBuffer keyBuffer = ByteBuffer.allocateDirect(50);
        basicLmdbDb.getKeySerde().serialize(keyBuffer, "MyKey");

        AtomicReference<ByteBuffer> valueBufRef = new AtomicReference<>();
        // now get the value again and it should be correct
        LmdbUtils.getWithReadTxn(lmdbEnv, txn -> {
            ByteBuffer valueBuffer = basicLmdbDb.getAsBytes(txn, keyBuffer).get();

            // hold on to the buffer for later
            valueBufRef.set(valueBuffer);

            String val = basicLmdbDb.getValueSerde().deserialize(valueBuffer);

            assertThat(val).isEqualTo("MyValue");

            return val;
        });

        // now mutate the value buffer outside any txn

        assertThat(valueBufRef.get().position()).isEqualTo(0);


        // This line will crash the JVM as we are mutating a directly allocated ByteBuffer
        // (that points to memory managed by LMDB) outside of a txn.
        basicLmdbDb.getValueSerde().serialize(valueBufRef.get(), "XXX");

    }

    @Test
    void testGetAsBytes() {

        basicLmdbDb.put("key1", "value1", false);
        basicLmdbDb.put("key2", "value2", false);
        basicLmdbDb.put("key3", "value3", false);

        LmdbUtils.doWithReadTxn(lmdbEnv, txn -> {
            Optional<ByteBuffer> optKeyBuffer = basicLmdbDb.getAsBytes(txn, "key2");

            assertThat(optKeyBuffer).isNotEmpty();
        });
    }

    @Test
    void testGetAsBytes2() {

        basicLmdbDb.put("key1", "value1", false);
        basicLmdbDb.put("key2", "value2", false);
        basicLmdbDb.put("key3", "value3", false);

        LmdbUtils.doWithReadTxn(lmdbEnv, txn -> {

            try (PooledByteBuffer pooledKeyBuffer = basicLmdbDb.getPooledKeyBuffer()) {
                ByteBuffer keyBuffer = pooledKeyBuffer.getByteBuffer();
                basicLmdbDb.serializeKey(keyBuffer, "key2");
                Optional<ByteBuffer> optValueBuffer = basicLmdbDb.getAsBytes(txn, keyBuffer);

                assertThat(optValueBuffer).isNotEmpty();
                String val = basicLmdbDb.deserializeKey(optValueBuffer.get());
                assertThat(val).isEqualTo("value2");
            }
        });
    }

    @Disabled // Until https://github.com/lmdbjava/lmdbjava/issues/169 is fixed and released
    @Test
    void testKeyRange() {

        basicLmdbDb.put("key11", "value1", false);
        basicLmdbDb.put("key12", "value1", false);
        basicLmdbDb.put("key13", "value1", false);
        basicLmdbDb.put("key21", "value2", false);
        basicLmdbDb.put("key22", "value2", false);
        basicLmdbDb.put("key23", "value2", false);
        basicLmdbDb.put("key31", "value3", false);
        basicLmdbDb.put("key32", "value3", false);
        basicLmdbDb.put("key33", "value3", false);

        LmdbUtils.doWithReadTxn(lmdbEnv, txn -> {

            try (PooledByteBuffer pooledStartKeyBuffer = basicLmdbDb.getPooledKeyBuffer();
                    PooledByteBuffer pooledEndKeyBuffer = basicLmdbDb.getPooledKeyBuffer()) {

                ByteBuffer startKeyBuffer = pooledStartKeyBuffer.getByteBuffer();
                ByteBuffer endKeyBuffer = pooledEndKeyBuffer.getByteBuffer();

                final String startKey = "key2";
                final String endKey = "key3";
                basicLmdbDb.serializeKey(startKeyBuffer, startKey);
                basicLmdbDb.serializeKey(endKeyBuffer, endKey);
                LOGGER.info("{} => {}",
                        ByteBufferUtils.byteBufferInfo(startKeyBuffer),
                        ByteBufferUtils.byteBufferInfo(endKeyBuffer));

                final KeyRange<ByteBuffer> keyRange = KeyRange.closedOpen(startKeyBuffer, endKeyBuffer);

                basicLmdbDb.forEachEntryAsBytes(txn, keyRange, kvTuple -> {
                    LOGGER.info("{} - {}",
                            ByteBufferUtils.byteBufferInfo(kvTuple.key()),
                            ByteBufferUtils.byteBufferInfo(kvTuple.val()));
                });

                final List<String> keysFound = new ArrayList<>();

                basicLmdbDb.forEachEntry(txn, KeyRange.closedOpen(startKey, endKey), kvTuple -> {
                    keysFound.add(kvTuple._1());
                });

                Assertions.assertThat(keysFound)
                        .containsExactly("key21", "key22", "key23");
            }
        });
    }

    @Test
    void testStreamEntries() {
        populateDb();

        List<String> entries = LmdbUtils.getWithReadTxn(lmdbEnv, txn ->
                basicLmdbDb.streamEntries(txn, KeyRange.all(), stream ->
                        stream
                                .map(kvTuple ->
                                        kvTuple._1() + "-" + kvTuple._2())
                                .peek(LOGGER::info)
                                .collect(Collectors.toList())));

        assertThat(entries).hasSize(20);
    }

    @Test
    void testStreamEntriesWithFilter() {
        populateDb();

        List<String> entries = LmdbUtils.getWithReadTxn(lmdbEnv, txn ->
                basicLmdbDb.streamEntries(txn, KeyRange.all(), stream ->
                        stream
                                .filter(kvTuple -> {
                                    int i = Integer.parseInt(kvTuple._1());
                                    return i > 10 && i <= 15;
                                })
                                .map(kvTuple ->
                                        kvTuple._1() + "-" + kvTuple._2())
                                .peek(LOGGER::info)
                                .collect(Collectors.toList())));

        assertThat(entries).hasSize(5);
    }


    @Test
    void testStreamEntriesWithKeyRange() {
        populateDb();

        KeyRange<String> keyRange = KeyRange.closed("06", "10");
        List<String> entries = LmdbUtils.getWithReadTxn(lmdbEnv, txn ->
                basicLmdbDb.streamEntries(txn, keyRange, stream ->
                        stream
                                .map(kvTuple ->
                                        kvTuple._1() + "-" + kvTuple._2())
                                .peek(LOGGER::info)
                                .collect(Collectors.toList())));

        assertThat(entries).hasSize(5);
    }

    @Test
    void testKeyReuse() {
        // key 1 => key2 => value2 & value 3
        basicLmdbDb.put("key1", "key2", false);
        basicLmdbDb.put("key2", "value2", false);

        // different DB with same key in it so we can test two lookups using same key
        basicLmdbDb2.put("key2", "value3", false);

        LmdbUtils.doWithReadTxn(lmdbEnv, txn -> {
            ByteBuffer keyBuffer = ByteBuffer.allocateDirect(100);
            basicLmdbDb.serializeKey(keyBuffer, "key1");
            ByteBuffer keyBufferCopy = keyBuffer.asReadOnlyBuffer();

            ByteBuffer valueBuffer = basicLmdbDb.getAsBytes(txn, keyBuffer).get();
            ByteBuffer keyBuffer2 = ByteBufferUtils.copyToDirectBuffer(valueBuffer);
            String value = basicLmdbDb.deserializeValue(valueBuffer);
            ByteBuffer valueBufferCopy = valueBuffer.asReadOnlyBuffer();

            assertThat(value).isEqualTo("key2");
            assertThat(keyBuffer).isEqualTo(keyBufferCopy);
            assertThat(valueBuffer).isEqualTo(valueBufferCopy);
            assertThat(valueBuffer).isEqualTo(keyBuffer2);

            // now use the value from the last get() as the key for a new get()
            ByteBuffer valueBuffer2 = basicLmdbDb.getAsBytes(txn, valueBuffer).get();

            String value2 = basicLmdbDb.deserializeValue(valueBuffer2);
            String valueBufferDeserialised = basicLmdbDb.deserializeKey(valueBuffer);

            assertThat(value2).isEqualTo("value2");

            // The second get() has overwritten our original valueBuffer with the value
            // of the second get(). This is because the txn essentially holds a cursor
            // whose position is updated by the get and that cursor is bound to the value
            // buffer returned by the get. The value from the get() can be used as a key
            // in another get() once, but that second get() will mutate it prevent it from
            // being used as a key in another get().
            assertThat(valueBufferDeserialised).isEqualTo("value2");
            assertThat(valueBuffer).isEqualTo(valueBuffer2);
            assertThat(valueBuffer).isNotEqualTo(valueBufferCopy);
            assertThat(keyBuffer2).isNotEqualTo(valueBuffer);

            // We can't use valueBuffer for our key here is it now points to "value2"
            ByteBuffer valueBuffer3 = basicLmdbDb2.getAsBytes(txn, keyBuffer2).get();

            String value3 = basicLmdbDb2.deserializeValue(valueBuffer3);

            assertThat(value3).isEqualTo("value3");
            assertThat(keyBuffer).isEqualTo(keyBufferCopy);
        });
    }

    @Test
    void testValueReuse() {
        basicLmdbDb.put("key1", "value1", false);
        basicLmdbDb.put("key2", "value2", false);

        LmdbUtils.doWithReadTxn(lmdbEnv, txn -> {

            ByteBuffer keyBuffer1 = ByteBuffer.allocateDirect(100);
            basicLmdbDb.serializeKey(keyBuffer1, "key1");

            ByteBuffer valueBuffer1 = basicLmdbDb.getAsBytes(txn, keyBuffer1).get();
            ByteBuffer valueBuffer1Copy = valueBuffer1.asReadOnlyBuffer();
            assertThat(valueBuffer1Copy).isEqualTo(valueBuffer1);
            String value1 = basicLmdbDb.deserializeValue(valueBuffer1);

            assertThat(value1).isEqualTo("value1");

            // now do another get on a different key to get a different value
            ByteBuffer keyBuffer2 = ByteBuffer.allocateDirect(100);
            basicLmdbDb.serializeKey(keyBuffer2, "key2");
            ByteBuffer valueBuffer2 = basicLmdbDb.getAsBytes(txn, keyBuffer2).get();

            String value2 = basicLmdbDb.deserializeValue(valueBuffer2);
            assertThat(value2).isEqualTo("value2");

            // The valueBuffer1 is tied to the txn's cursor which has now moved to key2 => value2,
            // thus valueBuffer1 now contains "value2".
            value1 = basicLmdbDb.deserializeValue(valueBuffer1);
            assertThat(value1).isEqualTo("value2");
            assertThat(valueBuffer1Copy).isNotEqualTo(valueBuffer1);
        });
    }

    @Test
    void testVerifyNumericKeyOrder() {

        // Ensure entries come back in the right order
        final List<Tuple2<Integer, String>> data = List.of(
                Tuple.of(1, "val1"),
                Tuple.of(2, "val2"),
                Tuple.of(3, "val3"),
                Tuple.of(4, "val4"));

        LmdbUtils.doWithWriteTxn(lmdbEnv, writeTxn -> {
            data.forEach(tuple -> {
                basicLmdbDb3.put(writeTxn, tuple._1(), tuple._2(), false);
            });
        });

        final KeyRange<Integer> keyRangeAll = KeyRange.all();

        final List<Integer> output = LmdbUtils.getWithReadTxn(lmdbEnv, readTxn ->
                basicLmdbDb3.streamEntries(readTxn, keyRangeAll, stream ->
                        stream
                                .map(Tuple2::_1)
                                .collect(Collectors.toList())));

        // Verify key order
        Assertions.assertThat(output)
                .containsExactlyElementsOf(data.stream()
                        .map(Tuple2::_1)
                        .collect(Collectors.toList()));
    }

    /**
     * This is more of a manual performance test for comparing the difference between
     * puts in integer order vs in random order. Also compares the impact of the INTEGER_KEY
     * dbi flag, which seems to slow things down a fair bit.
     */
    @Test
    void testLoadOrderAndIntKeyPerformance() {

//        final int iterations = 10_000_000;
        final int iterations = 10;

        LOGGER.info("info {}", basicLmdbDb3.getDbInfo());

        // Ensure entries come back in the right order
        final List<Tuple2<Integer, String>> ascendingData = IntStream
                .range(0, iterations)
                .boxed()
                .map(i -> Tuple.of(i, String.format("Val %010d", i)))
                .collect(Collectors.toList());

        Assertions.assertThat(ascendingData)
                .hasSize(iterations);

        Random random = new Random();
        final List<Tuple2<Integer, String>> randomData = IntStream
                .range(Integer.MAX_VALUE - iterations, Integer.MAX_VALUE)
                .boxed()
                .sorted(Comparator.comparingInt(i -> random.nextInt(iterations)))
                .map(i -> Tuple.of(i, String.format("Val %010d", i)))
                .collect(Collectors.toList());

        Assertions.assertThat(ascendingData)
                .hasSize(iterations);

        LOGGER.logDurationIfInfoEnabled(() -> {
            LmdbUtils.doWithWriteTxn(lmdbEnv, writeTxn -> {
                ascendingData.forEach(tuple -> {
                    basicLmdbDb3.put(writeTxn, tuple._1(), tuple._2(), false);
                });
            });
        }, "Ascending");

        LOGGER.logDurationIfInfoEnabled(() -> {
            LmdbUtils.doWithWriteTxn(lmdbEnv, writeTxn -> {
                randomData.forEach(tuple -> {
                    basicLmdbDb3.put(writeTxn, tuple._1(), tuple._2(), false);
                });
            });
        }, "Random");

        LOGGER.logDurationIfInfoEnabled(() -> {
            LmdbUtils.doWithWriteTxn(lmdbEnv, writeTxn -> {
                ascendingData.forEach(tuple -> {
                    basicLmdbDb4.put(writeTxn, tuple._1(), tuple._2(), false);
                });
            });
        }, "Ascending (INTEGER_KEY)");

        LOGGER.logDurationIfInfoEnabled(() -> {
            LmdbUtils.doWithWriteTxn(lmdbEnv, writeTxn -> {
                randomData.forEach(tuple -> {
                    basicLmdbDb4.put(writeTxn, tuple._1(), tuple._2(), false);
                });
            });
        }, "Random (INTEGER_KEY)");

        if (iterations < 50) {
            basicLmdbDb3.logDatabaseContents(LOGGER::info);
            basicLmdbDb3.logRawDatabaseContents(LOGGER::info);
            basicLmdbDb4.logDatabaseContents(LOGGER::info);
            basicLmdbDb4.logRawDatabaseContents(LOGGER::info);
        }
        LOGGER.info("entry count: " + basicLmdbDb3.getEntryCount());
        LOGGER.info("entry count: " + basicLmdbDb4.getEntryCount());
    }

    @Test
    void testMaxReaders() {
        Assertions.assertThat(lmdbEnv.info().numReaders)
                .isEqualTo(0);
        populateDb();
        // Show that writes to the db do not effect the num readers high water mark
        Assertions.assertThat(lmdbEnv.info().numReaders)
                .isEqualTo(0);
    }

    private void populateDb() {
        // pad the keys to a fixed length so they sort in number order
        IntStream.rangeClosed(1, 20).forEach(i -> {
            basicLmdbDb.put(buildKey(i), buildValue(i), false);

        });

        basicLmdbDb.logDatabaseContents(LOGGER::info);
    }

    private String buildKey(int i) {
        return String.format("%02d", i);
    }

    private String buildValue(int i) {
        return "value" + i;
    }

}
