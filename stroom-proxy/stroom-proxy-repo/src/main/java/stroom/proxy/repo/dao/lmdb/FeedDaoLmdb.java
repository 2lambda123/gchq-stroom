package stroom.proxy.repo.dao.lmdb;

import stroom.bytebuffer.ByteBufferUtils;
import stroom.proxy.repo.FeedKey;
import stroom.proxy.repo.dao.FeedDao;
import stroom.proxy.repo.dao.lmdb.serde.FeedKeySerde;
import stroom.proxy.repo.dao.lmdb.serde.LongSerde;
import stroom.util.io.PathCreator;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;

import java.nio.ByteBuffer;
import java.util.Optional;

@Singleton
public class FeedDaoLmdb implements FeedDao {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(FeedDaoLmdb.class);

    private final Env<ByteBuffer> env;
    private final Dbi<ByteBuffer> feedDbi;
    private final Dbi<ByteBuffer> indexDbi;
    private final LmdbWriteQueue writeQueue;
    private LongRowKey rowKey;

    private final LongSerde longSerde = new LongSerde();
    private final FeedKeySerde feedKeySerde = new FeedKeySerde();


    @Inject
    public FeedDaoLmdb(final ProxyLmdbConfig proxyLmdbConfig,
                       final PathCreator pathCreator,
                       final LmdbEnvFactory lmdbEnvFactory) {
        try {
            this.env = lmdbEnvFactory.build(pathCreator, proxyLmdbConfig, "feed");
            this.feedDbi = env.openDbi("feed", DbiFlags.MDB_CREATE);
            this.indexDbi = env.openDbi("feed-index", DbiFlags.MDB_CREATE);
            writeQueue = new LmdbWriteQueue(env);

            rowKey = new LongRowKey(env, feedDbi);
        } catch (final RuntimeException e) {
            LOGGER.error(e::getMessage, e);
            throw e;
        }
    }

    @Override
    public long getId(final FeedKey feedKey) {
        final ByteBuffer valueByteBuffer = feedKeySerde.serialise(feedKey);
        final long hash = ByteBufferUtils.xxHash(valueByteBuffer);
        final ByteBuffer hashByteBuffer = longSerde.serialise(hash);
        Optional<Long> id = fetchId(feedDbi, indexDbi, hashByteBuffer, valueByteBuffer);
        if (id.isPresent()) {
            return id.get();
        }

        synchronized (this) {
            // Try fetch under lock.
            id = fetchId(feedDbi, indexDbi, hashByteBuffer, valueByteBuffer);
            if (id.isPresent()) {
                return id.get();
            }

            // Couldn't fetch, try put.
            final long newId = rowKey.next();
            final ByteBuffer idByteBuffer = longSerde.serialise(newId);
            put(idByteBuffer, hashByteBuffer, valueByteBuffer);
            writeQueue.sync();

            return newId;
        }
    }

    private void put(final ByteBuffer idBuffer, final ByteBuffer hashByteBuffer, final ByteBuffer value) {
        writeQueue.write(txn -> {
            final ByteBuffer existingIndexValue = indexDbi.get(txn, hashByteBuffer);
            if (existingIndexValue != null) {
                final ByteBuffer appended;
                final int bufferSize = existingIndexValue.remaining() + Long.BYTES;
                try (final MyByteBufferOutput output = new MyByteBufferOutput(bufferSize, bufferSize)) {
                    output.writeByteBuffer(existingIndexValue);
                    output.writeByteBuffer(idBuffer);
                    output.flush();
                    appended = output.getByteBuffer().flip();
                }
                indexDbi.put(txn, hashByteBuffer, appended);
            } else {
                indexDbi.put(txn, hashByteBuffer, idBuffer);
            }

            feedDbi.put(txn, idBuffer, value);
        });
    }

    private Optional<Long> fetchId(final Dbi<ByteBuffer> dbi,
                                   final Dbi<ByteBuffer> indexDbi,
                                   final ByteBuffer hashByteBuffer,
                                   final ByteBuffer valueByteBuffer) {
        try (final Txn<ByteBuffer> txn = env.txnRead()) {
            final ByteBuffer value = indexDbi.get(txn, hashByteBuffer);
            if (value != null) {
                while (value.hasRemaining()) {
                    final long id = value.getLong();
                    final ByteBuffer index = longSerde.serialise(id);
                    final ByteBuffer storedValue = dbi.get(txn, index);
                    if (storedValue.equals(valueByteBuffer)) {
                        return Optional.of(id);
                    }
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public FeedKey getKey(final long id) {
        final ByteBuffer idByteBuffer = longSerde.serialise(id);
        try (final Txn<ByteBuffer> txn = env.txnRead()) {
            final ByteBuffer value = feedDbi.get(txn, idByteBuffer);
            if (value == null) {
                return null;
            }
            return feedKeySerde.deserialise(value);
        }
    }

    @Override
    public void clear() {
        writeQueue.write(txn -> LmdbUtil.deleteAll(txn, feedDbi));
        writeQueue.write(txn -> LmdbUtil.deleteAll(txn, indexDbi));
        writeQueue.commit();
        writeQueue.sync();

        rowKey = new LongRowKey(env, feedDbi);
    }

    @Override
    public int countFeeds() {
        try (final Txn<ByteBuffer> txn = env.txnRead()) {
            return (int) LmdbUtil.count(txn, feedDbi);
        }
    }
}
