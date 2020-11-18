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

package stroom.pipeline.refdata.store.offheapstore;

import stroom.docstore.shared.DocRefUtil;
import stroom.pipeline.refdata.ReferenceDataConfig;
import stroom.pipeline.refdata.store.AbstractRefDataStore;
import stroom.pipeline.refdata.store.MapDefinition;
import stroom.pipeline.refdata.store.ProcessingState;
import stroom.pipeline.refdata.store.RefDataLoader;
import stroom.pipeline.refdata.store.RefDataProcessingInfo;
import stroom.pipeline.refdata.store.RefDataStore;
import stroom.pipeline.refdata.store.RefDataValue;
import stroom.pipeline.refdata.store.RefDataValueConverter;
import stroom.pipeline.refdata.store.RefStoreEntry;
import stroom.pipeline.refdata.store.RefStreamDefinition;
import stroom.pipeline.refdata.store.offheapstore.databases.KeyValueStoreDb;
import stroom.pipeline.refdata.store.offheapstore.databases.KeyValueStoreDb.Factory;
import stroom.pipeline.refdata.store.offheapstore.databases.MapUidForwardDb;
import stroom.pipeline.refdata.store.offheapstore.databases.MapUidReverseDb;
import stroom.pipeline.refdata.store.offheapstore.databases.ProcessingInfoDb;
import stroom.pipeline.refdata.store.offheapstore.databases.RangeStoreDb;
import stroom.pipeline.refdata.store.offheapstore.databases.ValueStoreDb;
import stroom.pipeline.refdata.store.offheapstore.databases.ValueStoreMetaDb;
import stroom.pipeline.refdata.store.offheapstore.lmdb.LmdbDb;
import stroom.pipeline.refdata.store.offheapstore.lmdb.LmdbUtils;
import stroom.pipeline.refdata.store.offheapstore.serdes.RefDataProcessingInfoSerde;
import stroom.pipeline.refdata.util.ByteBufferPool;
import stroom.pipeline.refdata.util.ByteBufferUtils;
import stroom.pipeline.refdata.util.PooledByteBuffer;
import stroom.pipeline.refdata.util.PooledByteBufferPair;
import stroom.util.io.PathCreator;
import stroom.util.HasHealthCheck;
import stroom.util.io.ByteSize;
import stroom.util.io.TempDirProvider;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.shared.ModelStringUtil;
import stroom.util.sysinfo.HasSystemInfo;
import stroom.util.sysinfo.SystemInfoResult;
import stroom.util.time.StroomDuration;
import stroom.util.time.TimeUtils;

import com.google.common.util.concurrent.Striped;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.hadoop.hbase.util.Bytes;
import org.jetbrains.annotations.NotNull;
import org.lmdbjava.Env;
import org.lmdbjava.EnvFlags;
import org.lmdbjava.KeyRange;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An Off Heap implementation of {@link RefDataStore} using LMDB.
 *
 * Essentially each ref stream (a {@link RefStreamDefinition}) contains 1-* map names.
 * Multiple ref streams can contain the same map name.
 * Within a ref stream + map combo (a {@link MapDefinition}) there are 1-* entries.
 * A lookup is done by creating a key that is the lookup key combined with a {@link MapDefinition}
 * so only an entry matching the lookup key, the map name and the ref stream will be returned.
 */
@Singleton
public class RefDataOffHeapStore extends AbstractRefDataStore implements RefDataStore, HasSystemInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefDataOffHeapStore.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(RefDataOffHeapStore.class);

    private static final String DEFAULT_STORE_SUB_DIR_NAME = "refDataOffHeapStore";

    private static final long PROCESSING_INFO_UPDATE_DELAY_MS = Duration.of(1, ChronoUnit.HOURS).toMillis();

    // These are dups of org.lmdbjava.Library.LMDB_* but that class is pkg private for some reason.
    private static final String LMDB_EXTRACT_DIR_PROP = "lmdbjava.extract.dir";
    private static final String LMDB_NATIVE_LIB_PROP = "lmdbjava.native.lib";

    private final TempDirProvider tempDirProvider;
    private final PathCreator pathCreator;
    private final Path dbDir;
    private final ByteSize maxSize;
    private final int maxReaders;
    private final int maxPutsBeforeCommit;

    private final Env<ByteBuffer> lmdbEnvironment;

    // the DBs that make up the store
    private final KeyValueStoreDb keyValueStoreDb;
    private final RangeStoreDb rangeStoreDb;
    private final ProcessingInfoDb processingInfoDb;

    // classes that front multiple DBs
    private final ValueStore valueStore;
    private final MapDefinitionUIDStore mapDefinitionUIDStore;

    private final ReferenceDataConfig referenceDataConfig;

    private final RefDataValueConverter refDataValueConverter;

    private final Map<String, LmdbDb> databaseMap = new HashMap<>();

    // For synchronising access to the data belonging to a MapDefinition
    private final Striped<Lock> refStreamDefStripedReentrantLock;

    private final ByteBufferPool byteBufferPool;

    @Inject
    RefDataOffHeapStore(
            final TempDirProvider tempDirProvider,
            final PathCreator pathCreator,
            final ReferenceDataConfig referenceDataConfig,
            final ByteBufferPool byteBufferPool,
            final Factory keyValueStoreDbFactory,
            final ValueStoreDb.Factory valueStoreDbFactory,
            final ValueStoreMetaDb.Factory valueStoreMetaDbFactory,
            final RangeStoreDb.Factory rangeStoreDbFactory,
            final MapUidForwardDb.Factory mapUidForwardDbFactory,
            final MapUidReverseDb.Factory mapUidReverseDbFactory,
            final RefDataValueConverter refDataValueConverter,
            final ProcessingInfoDb.Factory processingInfoDbFactory) {

        this.tempDirProvider = tempDirProvider;
        this.pathCreator = pathCreator;
        this.referenceDataConfig = referenceDataConfig;
        this.refDataValueConverter = refDataValueConverter;
        this.dbDir = getStoreDir();
        this.maxSize = referenceDataConfig.getMaxStoreSize();
        this.maxReaders = referenceDataConfig.getMaxReaders();
        this.maxPutsBeforeCommit = referenceDataConfig.getMaxPutsBeforeCommit();

        this.lmdbEnvironment = createEnvironment(referenceDataConfig);

        // create all the databases
        this.keyValueStoreDb = keyValueStoreDbFactory.create(lmdbEnvironment);
        this.rangeStoreDb = rangeStoreDbFactory.create(lmdbEnvironment);
        final ValueStoreDb valueStoreDb = valueStoreDbFactory.create(lmdbEnvironment);
        final MapUidForwardDb mapUidForwardDb = mapUidForwardDbFactory.create(lmdbEnvironment);
        final MapUidReverseDb mapUidReverseDb = mapUidReverseDbFactory.create(lmdbEnvironment);
        this.processingInfoDb = processingInfoDbFactory.create(lmdbEnvironment);
        final ValueStoreMetaDb valueStoreMetaDb = valueStoreMetaDbFactory.create(lmdbEnvironment);

        // hold all the DBs in a map so we can get at them by name
        addDbsToMap(
                keyValueStoreDb,
                rangeStoreDb,
                valueStoreDb,
                mapUidForwardDb,
                mapUidReverseDb,
                processingInfoDb,
                valueStoreMetaDb);

        this.valueStore = new ValueStore(lmdbEnvironment, valueStoreDb, valueStoreMetaDb);
        this.mapDefinitionUIDStore = new MapDefinitionUIDStore(lmdbEnvironment, mapUidForwardDb, mapUidReverseDb);

        this.byteBufferPool = byteBufferPool;

        this.refStreamDefStripedReentrantLock = Striped.lazyWeakLock(100);
    }

    private Env<ByteBuffer> createEnvironment(final ReferenceDataConfig referenceDataConfig) {
        LOGGER.info(
                "Creating RefDataOffHeapStore environment with [maxSize: {}, dbDir {}, maxReaders {}, " +
                        "maxPutsBeforeCommit {}, isReadAheadEnabled {}]",
                maxSize,
                dbDir.toAbsolutePath().toString() + File.separatorChar,
                maxReaders,
                maxPutsBeforeCommit,
                referenceDataConfig.isReadAheadEnabled());

        // By default LMDB opens with readonly mmaps so you cannot mutate the bytebuffers inside a txn.
        // Instead you need to create a new bytebuffer for the value and put that. If you want faster writes
        // then you can use EnvFlags.MDB_WRITEMAP in the open() call to allow mutation inside a txn but that
        // comes with greater risk of corruption.

        // NOTE on setMapSize() from LMDB author found on https://groups.google.com/forum/#!topic/caffe-users/0RKsTTYRGpQ
        // On Windows the OS sets the filesize equal to the mapsize. (MacOS requires that too, and allocates
        // all of the physical space up front, it doesn't support sparse files.) The mapsize should not be
        // hardcoded into software, it needs to be reconfigurable. On Windows and MacOS you really shouldn't
        // set it larger than the amount of free space on the filesystem.

        final EnvFlags[] envFlags;
        if (referenceDataConfig.isReadAheadEnabled()) {
            envFlags = new EnvFlags[0];
        } else {
            envFlags = new EnvFlags[]{EnvFlags.MDB_NORDAHEAD};
        }

        final String lmdbSystemLibraryPath = referenceDataConfig.getLmdbSystemLibraryPath();

        if (lmdbSystemLibraryPath != null) {
            // javax.validation should ensure the path is valid if set
            System.setProperty(LMDB_NATIVE_LIB_PROP, lmdbSystemLibraryPath);
            LOGGER.info("Using provided LMDB system library file " + lmdbSystemLibraryPath);
        } else {
            // Set the location to extract the bundled LMDB binary to
            System.setProperty(LMDB_EXTRACT_DIR_PROP, dbDir.toAbsolutePath().toString());
            LOGGER.info("Extracting bundled LMDB binary to " + dbDir);
        }

        final Env<ByteBuffer> env = Env.create()
                .setMaxReaders(maxReaders)
                .setMapSize(maxSize.getBytes())
                .setMaxDbs(7) //should equal the number of DBs we create which is fixed at compile time
                .open(dbDir.toFile(), envFlags);

        LOGGER.info("Existing databases: [{}]",
                env.getDbiNames()
                        .stream()
                        .map(Bytes::toString)
                        .collect(Collectors.joining(",")));
        return env;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.OFF_HEAP;
    }

    private void addDbsToMap(final LmdbDb... lmdbDbs) {
        for (LmdbDb lmdbDb : lmdbDbs) {
            this.databaseMap.put(lmdbDb.getDbName(), lmdbDb);
        }
    }

    @Override
    public Optional<RefDataProcessingInfo> getAndTouchProcessingInfo(final RefStreamDefinition refStreamDefinition) {
        // get the current processing info
        final Optional<RefDataProcessingInfo> optProcessingInfo = processingInfoDb.get(refStreamDefinition);

        // update the last access time, but only do it if it has been a while since we last did it to avoid
        // opening writeTxn all the time. The last accessed time is not critical as far as accuracy goes. As long
        // as it is reasonably accurate we can use it for purging old data.
        optProcessingInfo.ifPresent(processingInfo -> {
            long timeSinceLastAccessedTimeMs = System.currentTimeMillis() - processingInfo.getLastAccessedTimeEpochMs();
            if (timeSinceLastAccessedTimeMs > PROCESSING_INFO_UPDATE_DELAY_MS) {
                processingInfoDb.updateLastAccessedTime(refStreamDefinition);
            }
        });
        LOGGER.trace("getProcessingInfo({}) - {}", refStreamDefinition, optProcessingInfo);
        return optProcessingInfo;
    }

    @Override
    public boolean isDataLoaded(final RefStreamDefinition refStreamDefinition) {

        boolean result = getAndTouchProcessingInfo(refStreamDefinition)
                .map(RefDataProcessingInfo::getProcessingState)
                .filter(Predicate.isEqual(ProcessingState.COMPLETE))
                .isPresent();

        LOGGER.trace("isDataLoaded({}) - {}", refStreamDefinition, result);
        return result;
    }

    /**
     * Returns true if this {@link MapDefinition} exists in the store. It makes no guarantees about the state
     * of the data.
     *
     * @param mapDefinition
     */
    @Override
    public boolean exists(final MapDefinition mapDefinition) {
        return mapDefinitionUIDStore.exists(mapDefinition);
    }

    @Override
    public Optional<RefDataValue> getValue(final MapDefinition mapDefinition,
                                           final String key) {

        // Use the mapDef to get a mapUid, then use the mapUid and key
        // to do a lookup in the keyValue or rangeValue stores. The resulting
        // value store key buffer can then be used to get the actual value.
        // The value is then deserialised while still inside the txn.
        try (PooledByteBuffer valueStoreKeyPooledBufferClone = valueStore.getPooledKeyBuffer()) {
            Optional<RefDataValue> optionalRefDataValue =
                    LmdbUtils.getWithReadTxn(lmdbEnvironment, readTxn ->
                            // Perform the lookup with the map+key. The returned value (if found)
                            // is the key of the ValueStore, which we can use to find the actual
                            // value.
                            getValueStoreKey(readTxn, mapDefinition, key)
                                    .flatMap(valueStoreKeyBuffer -> {
                                        // we are going to use the valueStoreKeyBuffer as a key in multiple
                                        // get() calls so need to clone it first.
                                        final ByteBuffer valueStoreKeyBufferClone = valueStoreKeyPooledBufferClone.getByteBuffer();
                                        ByteBufferUtils.copy(valueStoreKeyBuffer, valueStoreKeyBufferClone);
                                        return Optional.of(valueStoreKeyBufferClone);
                                    })
                                    .flatMap(valueStoreKeyBuffer ->
                                            valueStore.get(readTxn, valueStoreKeyBuffer)));

            LOGGER.trace("getValue({}, {}) - {}", mapDefinition, key, optionalRefDataValue);
            return optionalRefDataValue;
        }
    }

    /**
     * Intended only for testing use.
     */
    void setLastAccessedTime(final RefStreamDefinition refStreamDefinition, long timeMs) {
        processingInfoDb.updateLastAccessedTime(refStreamDefinition, timeMs);
    }

    private Optional<ByteBuffer> getValueStoreKey(final Txn<ByteBuffer> readTxn,
                                                  final MapDefinition mapDefinition,
                                                  final String key) {
        LOGGER.trace("getValueStoreKey({}, {})", mapDefinition, key);

        // TODO we could could consider a short lived on-heap cache for this as it
        // will be hit MANY times for the same entry
        final Optional<UID> optMapUid = mapDefinitionUIDStore.get(readTxn, mapDefinition);

        Optional<ByteBuffer> optValueStoreKeyBuffer;
        if (optMapUid.isPresent()) {
            LOGGER.trace("Found map UID {}", optMapUid);
            //do the lookup in the kv store first
            final UID mapUid = optMapUid.get();
            final KeyValueStoreKey keyValueStoreKey = new KeyValueStoreKey(optMapUid.get(), key);

            optValueStoreKeyBuffer = keyValueStoreDb.getAsBytes(readTxn, keyValueStoreKey);

            if (optValueStoreKeyBuffer.isEmpty()) {
                //not found in the kv store so look in the key range store instead

                try {
                    // speculative lookup in the range store. At this point we don't know if we have
                    // any ranges for this mapdef or not, but either way we need a call to LMDB so
                    // just do the range lookup
                    final long keyLong = Long.parseLong(key);

                    // look up our long key in the range store to see if it is part of a range
                    optValueStoreKeyBuffer = rangeStoreDb.getAsBytes(readTxn, mapUid, keyLong);

                } catch (NumberFormatException e) {
                    // key could not be converted to a long, either this mapdef has no ranges or
                    // an invalid key was used. See if we have any ranges at all for this mapdef
                    // to determine whether to error or not.
                    boolean doesStoreContainRanges = rangeStoreDb.containsMapDefinition(readTxn, mapUid);
                    if (doesStoreContainRanges) {
                        // we have ranges for this map def so we would expect to be able to convert the key
                        throw new RuntimeException(LogUtil.message(
                                "Key {} cannot be used with the range store as it cannot be converted to a long", key), e);
                    }
                    // no ranges for this map def so the fact that we could not convert the key to a long
                    // is not a problem. Do nothing.
                }
            }
        } else {
            LOGGER.debug("Couldn't find map UID which means the data for this map has " +
                            "not been loaded or the map name is wrong {}",
                    mapDefinition);
            // no map UID so can't look in key/range stores without one
            optValueStoreKeyBuffer = Optional.empty();
        }
        return optValueStoreKeyBuffer;
    }


    @Override
    public boolean consumeValueBytes(final MapDefinition mapDefinition,
                                     final String key,
                                     final Consumer<TypedByteBuffer> valueBytesConsumer) {

        // lookup the passed mapDefinition and key and if a valueStoreKey is found use that to
        // lookup the value in the value store, passing the actual value part to the consumer.
        // The consumer gets only the value, not the type or ref count and has to understand how
        // to interpret the bytes in the buffer

        try (PooledByteBuffer valueStoreKeyPooledBufferClone = valueStore.getPooledKeyBuffer()) {
            boolean wasValueFound = LmdbUtils.getWithReadTxn(lmdbEnvironment, txn ->
                    getValueStoreKey(txn, mapDefinition, key)
                            .flatMap(valueStoreKeyBuffer -> {
                                // we are going to use the valueStoreKeyBuffer as a key in multiple
                                // get() calls so need to clone it first.
                                final ByteBuffer valueStoreKeyBufferClone = valueStoreKeyPooledBufferClone.getByteBuffer();
                                ByteBufferUtils.copy(valueStoreKeyBuffer, valueStoreKeyBufferClone);
                                return Optional.of(valueStoreKeyBufferClone);
                            })
                            .flatMap(valueStoreKeyBuf ->
                                    valueStore.getTypedValueBuffer(txn, valueStoreKeyBuf))
                            .map(valueBuf -> {
                                valueBytesConsumer.accept(valueBuf);
                                return true;
                            })
                            .orElse(false));

            LOGGER.trace("consumeValueBytes({}, {}) - {}", mapDefinition, key, wasValueFound);
            return wasValueFound;
        }
    }

    @Override
    public void purgeOldData() {
        purgeOldData(Instant.now(), referenceDataConfig.getPurgeAge());
    }

    @Override
    public void purgeOldData(final StroomDuration purgeAge) {
        purgeOldData(Instant.now(), purgeAge);
    }

    /**
     * Get an instance of a {@link RefDataLoader} for bulk loading multiple entries for a given
     * {@link RefStreamDefinition} and its associated effectiveTimeMs. The {@link RefDataLoader}
     * should be used in a try with resources block to ensure any transactions are closed, e.g.
     * <pre>try (RefDataLoader refDataLoader = refDataOffHeapStore.getLoader(...)) { ... }</pre>
     */
    @Override
    protected RefDataLoader loader(final RefStreamDefinition refStreamDefinition,
                                   final long effectiveTimeMs) {
        //TODO should we pass in an ErrorReceivingProxy so we can log errors with it?
        RefDataLoader refDataLoader = new OffHeapRefDataLoader(
                this,
                refStreamDefStripedReentrantLock,
                keyValueStoreDb,
                rangeStoreDb,
                valueStore,
                mapDefinitionUIDStore,
                processingInfoDb,
                lmdbEnvironment,
                refStreamDefinition,
                effectiveTimeMs);

        refDataLoader.setCommitInterval(maxPutsBeforeCommit);
        return refDataLoader;
    }

    @Override
    public long getKeyValueEntryCount() {
        return keyValueStoreDb.getEntryCount();
    }

    @Override
    public long getKeyRangeValueEntryCount() {
        return rangeStoreDb.getEntryCount();
    }

    @Override
    public long getProcessingInfoEntryCount() {
        return processingInfoDb.getEntryCount();
    }

    /**
     * @param now Allows the setting of the current time for testing purposes
     */
    void purgeOldData(final Instant now, final StroomDuration purgeAge) {
        final Instant startTime = Instant.now();
        final AtomicReference<PurgeCounts> countsRef = new AtomicReference<>(PurgeCounts.zero());

        try (final PooledByteBuffer accessTimeThresholdPooledBuf = getAccessTimeCutOffBuffer(now, purgeAge);
             final PooledByteBufferPair procInfoPooledBufferPair = processingInfoDb.getPooledBufferPair()) {

            final AtomicReference<ByteBuffer> currRefStreamDefBufRef = new AtomicReference<>();
            final ByteBuffer accessTimeThresholdBuf = accessTimeThresholdPooledBuf.getByteBuffer();

            Predicate<ByteBuffer> accessTimePredicate = processingInfoBuffer ->
                    !RefDataProcessingInfoSerde.wasAccessedAfter(
                            processingInfoBuffer,
                            accessTimeThresholdBuf);

            final AtomicBoolean wasMatchFound = new AtomicBoolean(false);
            do {
                // with a read txn find the next proc info entry that is ready for purge
                Optional<RefStreamDefinition> optRefStreamDef = LmdbUtils.getWithReadTxn(lmdbEnvironment, readTxn -> {
                    // ensure the buffers are cleared as we are using them in a loop
                    return findNextRefStreamDef(
                            procInfoPooledBufferPair,
                            currRefStreamDefBufRef,
                            accessTimePredicate,
                            readTxn);
                });

                if (optRefStreamDef.isPresent()) {

                    LOGGER.debug("Found at least one refStreamDef ready for purge, now getting lock");

                    // now acquire a lock for the this ref stream def so we don't conflict with any load operations
                    doWithRefStreamDefinitionLock(refStreamDefStripedReentrantLock, optRefStreamDef.get(), () -> {
                        // start a write txn and re-fetch the next entry for purge (should be the same one as above)
                        // TODO we currently purge a whole refStreamDef in one txn, may be better to do it in smaller
                        // chunks
                        boolean wasFound = LmdbUtils.getWithWriteTxn(lmdbEnvironment, writeTxn -> {

                            final Optional<PooledByteBufferPair> optProcInfoBufferPair =
                                    processingInfoDb.getNextEntryAsBytes(
                                            writeTxn,
                                            currRefStreamDefBufRef.get(),
                                            accessTimePredicate,
                                            procInfoPooledBufferPair);

                            if (optProcInfoBufferPair.isEmpty()) {
                                // no matching ref streams found so break out
                                LOGGER.debug("No match found");
                                return false;
                            } else {
                                // found a ref stream def that is ready for purge
                                final ByteBuffer refStreamDefBuffer = optProcInfoBufferPair.get().getKeyBuffer();
                                final ByteBuffer refDataProcInfoBuffer = optProcInfoBufferPair.get().getValueBuffer();

                                // update this for the next iteration
                                currRefStreamDefBufRef.set(refStreamDefBuffer);

                                final RefStreamDefinition refStreamDefinition = processingInfoDb.deserializeKey(
                                        refStreamDefBuffer);
                                final RefDataProcessingInfo refDataProcessingInfo = processingInfoDb.deserializeValue(
                                        refDataProcInfoBuffer);

                                final String refStreamDefStr = LogUtil.message(
                                        "stream {}, " +
                                                "effective time {}, " +
                                                "pipeline {}, " +
                                                "pipeline version {}, " +
                                                "create time {}, " +
                                                "access time {}",
                                        refStreamDefinition.getStreamId(),
                                        refDataProcessingInfo.getEffectiveTime(),
                                        DocRefUtil.createSimpleDocRefString(refStreamDefinition.getPipelineDocRef()),
                                        refStreamDefinition.getPipelineVersion(),
                                        refDataProcessingInfo.getCreateTime(),
                                        refDataProcessingInfo.getLastAccessedTime());

                                LOGGER.info("  Purging refStreamDefinition with {}", refStreamDefStr);

                                // mark it is purge in progress
                                processingInfoDb.updateProcessingState(writeTxn,
                                        refStreamDefBuffer,
                                        ProcessingState.PURGE_IN_PROGRESS,
                                        false);

                                // purge the data associated with this ref stream def
                                final RefStreamPurgeCounts refStreamSummaryInfo = purgeRefStreamData(
                                        writeTxn, refStreamDefinition);

                                // aggregate the counts
                                countsRef.getAndUpdate(counts ->
                                        counts.increment(refStreamSummaryInfo));

                                //now delete the proc info entry
                                LOGGER.debug("Deleting processing info entry for {}", refStreamDefinition);

                                boolean didDeleteSucceed = processingInfoDb.delete(writeTxn, refStreamDefBuffer);

                                if (!didDeleteSucceed) {
                                    throw new RuntimeException("Processing info entry not found so was not deleted");
                                }

                                LOGGER.info("  Completed purge of refStreamDefinition with stream {} (" +
                                                "{} maps deleted, {} values deleted, {} values de-referenced)",
                                        refStreamDefinition.getStreamId(),
                                        refStreamSummaryInfo.mapsDeletedCount,
                                        refStreamSummaryInfo.valuesDeletedCount,
                                        refStreamSummaryInfo.valuesDeReferencedCount);
                                return true;
                            }
                        });
                        wasMatchFound.set(wasFound);
                    });
                } else {
                    wasMatchFound.set(false);
                }
            } while (wasMatchFound.get());
        }

        final PurgeCounts purgeCounts = countsRef.get();
        LOGGER.info("Purge completed in {}, {} refStreamDefs purged, " +
                        "{} maps deleted, {} values deleted, {} values de-referenced",
                Duration.between(startTime, Instant.now()),
                purgeCounts.refStreamDefsDeletedCount,
                purgeCounts.refStreamPurgeCounts.mapsDeletedCount,
                purgeCounts.refStreamPurgeCounts.valuesDeletedCount,
                purgeCounts.refStreamPurgeCounts.valuesDeReferencedCount);

        //open a write txn
        //open a cursor on the process info table to scan all records
        //subtract purge age prop val from current time to give purge cut off ms
        //for each proc info record one test the last access time against the cut off time (without de-serialising to long)
        //if it is older than cut off date then change its state to PURGE_IN_PROGRESS


        //process needs to be idempotent so we can continue a part finished purge. A new txn MUST always check the
        //processing info state to ensure it is still PURGE_IN_PROGRESS in case another txn has started a load, in which
        //case we won't purge. A purge txn must wrap at least the deletion of the key(range)/entry, the value (if no other
        //refs). The deletion of the mapdef<=>uid paiur must be done in a txn to ensure consistency.
        //Each processing info entry should be be fetched with a read txn, then get a StripedSemaphore for the streamdef
        //then open the write txn. This should stop any conflict with load jobs for that stream.

        //when overwrites happen we may have two values that had an association with same mapDef + key.  The above process
        //will only remove the currently associated value.  We would have to scan the whole value table to look for


        // streamDef => mapDefs
        // mapDef => mapUID
        // mapUID => ValueKeys
        // ValueKey => value

        // <pipe uuid 2-18><pipe ver 2-18><stream id 8> => <create time 8><last access time 8><effective time 8><state 1>
        // <pipe uuid 12-18><pipe ver 2-18><stream id 8><map name ?> => <mapUID 4>
        // <mapUID 4> => <pipe uuid 2-18><pipe ver 2-18><stream id 8><map name ?>
        // <mapUID 4><string Key ?> => <valueHash 4><id 2>
        // <mapUID 4><range start 8><range end 8> => <valueHash 4><id 2>
        // <valueHash 4><id 2> => <value type 1><reference count 4><value bytes ?>

        // increment ref count when
        // - putting new key(Range)/Value entry + new value entry (set initial ref count at 1)
        // - putting new key(Range)/Value entry with existing value entry
        // - overwrite key(Range)/Value entry (+1 on new value key)

        // decrement ref count when
        // - overwrite key(Range)/Value entry (-1 on old value key)
        // - delete key(Range)/Value entry

        // change to ref counter MUST be done in same txn as the thing that is making it change, e.g the KV entry removal
    }

    @NotNull
    private Optional<RefStreamDefinition> findNextRefStreamDef(final PooledByteBufferPair procInfoPooledBufferPair,
                                                               final AtomicReference<ByteBuffer> currRefStreamDefBufRef,
                                                               final Predicate<ByteBuffer> accessTimePredicate,
                                                               final Txn<ByteBuffer> readTxn) {
        procInfoPooledBufferPair.clear();
        Optional<PooledByteBufferPair> optProcInfoBufferPair = processingInfoDb.getNextEntryAsBytes(
                readTxn,
                currRefStreamDefBufRef.get(),
                accessTimePredicate,
                procInfoPooledBufferPair);

        return optProcInfoBufferPair.map(procInfoBufferPair -> {
            RefStreamDefinition refStreamDefinition = processingInfoDb.deserializeKey(
                    procInfoBufferPair.getKeyBuffer());

            // update the current key buffer so we can search from here next time
            currRefStreamDefBufRef.set(procInfoBufferPair.getKeyBuffer());
            return refStreamDefinition;
        });
    }

    private RefStreamPurgeCounts purgeRefStreamData(final Txn<ByteBuffer> writeTxn,
                                                    final RefStreamDefinition refStreamDefinition) {

        LOGGER.debug("purgeRefStreamData({})", refStreamDefinition);

        RefStreamPurgeCounts summaryInfo = RefStreamPurgeCounts.zero();
        Optional<UID> optMapUid;
        try (PooledByteBuffer pooledUidBuffer = byteBufferPool.getPooledByteBuffer(UID.UID_ARRAY_LENGTH)) {
            do {
                //open a ranged cursor on the map forward table to scan all map defs for that stream def
                //for each map def get the map uid
                optMapUid = mapDefinitionUIDStore.getNextMapDefinition(
                        writeTxn, refStreamDefinition, pooledUidBuffer::getByteBuffer);

                if (optMapUid.isPresent()) {
                    final UID mapUid = optMapUid.get();
                    final MapDefinition mapDefinition = mapDefinitionUIDStore.get(writeTxn, mapUid)
                            .orElseThrow(() ->
                                    new RuntimeException(LogUtil.message(
                                            "We should be a mapDefinition if we have a UID, uid: {}",
                                            mapUid)));

                    LOGGER.debug("Found mapUid {} for refStreamDefinition {}", mapUid, refStreamDefinition);
                    Tuple2<Integer, Integer> dataPurgeCounts = purgeMapData(writeTxn, optMapUid.get());
                    summaryInfo = summaryInfo.increment(
                            1,
                            dataPurgeCounts._1(),
                            dataPurgeCounts._2());

                    LOGGER.info("    Purged map {}, {} values deleted, {} values de-referenced",
                            mapDefinition.getMapName(),
                            dataPurgeCounts._1(),
                            dataPurgeCounts._2());
                } else {
                    LOGGER.debug("No more map definitions to purge for refStreamDefinition {}", refStreamDefinition);
                }
            } while (optMapUid.isPresent());
        }

//        LOGGER.info("Purged data for {} map(s) for {}", cnt, refStreamDefinition);
        return summaryInfo;
    }

    private Tuple2<Integer, Integer> purgeMapData(final Txn<ByteBuffer> writeTxn,
                                                  final UID mapUid) {

        LOGGER.debug("purgeMapData(writeTxn, {})", mapUid);

        LOGGER.debug("Deleting key/value entries and de-referencing/deleting their values");
        // loop over all keyValue entries for this mapUid and dereference/delete the associated
        // valueStore entry
        AtomicLong valueEntryDeleteCount = new AtomicLong();
        AtomicLong valueEntryDeReferenceCount = new AtomicLong();
        keyValueStoreDb.deleteMapEntries(writeTxn, mapUid, (keyValueStoreKeyBuffer, valueStoreKeyBuffer) -> {

            //dereference this value, deleting it if required
            deReferenceOrDeleteValue(writeTxn, valueStoreKeyBuffer, valueEntryDeleteCount, valueEntryDeReferenceCount);
        });
        LAMBDA_LOGGER.debug(() -> LogUtil.message("Deleted {} value entries, de-referenced {} value entries",
                valueEntryDeleteCount.get(), valueEntryDeReferenceCount.get()));

        LOGGER.debug("Deleting range/value entries and de-referencing/deleting their values");
        // loop over all rangeValue entries for this mapUid and dereference/delete the associated
        // valueStore entry
        rangeStoreDb.deleteMapEntries(writeTxn, mapUid, (writeTxn2, rangeValueStoreKeyBuffer, valueStoreKeyBuffer) -> {

            //dereference this value, deleting it if required
            deReferenceOrDeleteValue(writeTxn2, valueStoreKeyBuffer, valueEntryDeleteCount, valueEntryDeReferenceCount);
        });
        LOGGER.debug("Deleting range/value entries and de-referencing/deleting their values");

        mapDefinitionUIDStore.deletePair(writeTxn, mapUid);

        return Tuple.of(valueEntryDeleteCount.intValue(), valueEntryDeReferenceCount.intValue());
    }

    private void deReferenceOrDeleteValue(final Txn<ByteBuffer> writeTxn,
                                          final ByteBuffer valueStoreKeyBuffer,
                                          final AtomicLong valueEntryDeleteCount,
                                          final AtomicLong valueEntryDeReferenceCount) {

        boolean wasDeleted = valueStore.deReferenceOrDeleteValue(writeTxn, valueStoreKeyBuffer);

        if (wasDeleted) {
            // we deleted the meta entry so now delete the value entry
            valueEntryDeleteCount.incrementAndGet();
        } else {
            // keep a count of the de-reference
            valueEntryDeReferenceCount.incrementAndGet();
        }
    }


    /**
     * Package-private for testing
     */
    void doWithRefStreamDefinitionLock(final RefStreamDefinition refStreamDefinition, final Runnable work) {
        doWithRefStreamDefinitionLock(refStreamDefStripedReentrantLock, refStreamDefinition, work);
    }

    /**
     * For use in testing at SMALL scale. Dumps the content of each DB to the logger.
     */
    @Override
    public void logAllContents() {
        logAllContents(LOGGER::debug);
    }

    /**
     * For use in testing at SMALL scale. Dumps the content of each DB to the logger.
     */
    @Override
    public void logAllContents(Consumer<String> logEntryConsumer) {
        databaseMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .forEach(lmdbDb -> lmdbDb.logDatabaseContents(logEntryConsumer));
    }

    void logContents(final String dbName) {
        doWithLmdb(dbName, LmdbDb::logDatabaseContents);
    }

    void doWithLmdb(final String dbName, final Consumer<LmdbDb> work) {
        LmdbDb lmdbDb = databaseMap.get(dbName);
        if (lmdbDb == null) {
            throw new IllegalArgumentException(LogUtil.message("No database with name {} exists", dbName));
        }
        work.accept(lmdbDb);
    }

    /**
     * For use in testing at SMALL scale. Dumps the content of each DB to the logger.
     */
    void logAllRawContents(Consumer<String> logEntryConsumer) {
        databaseMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .forEach(lmdbDb -> lmdbDb.logDatabaseContents(logEntryConsumer));
    }

    void logRawContents(final String dbName) {
        doWithLmdb(dbName, LmdbDb::logRawDatabaseContents);
    }

    long getEntryCount(final String dbName) {
        LmdbDb lmdbDb = databaseMap.get(dbName);
        if (lmdbDb == null) {
            throw new IllegalArgumentException(LogUtil.message("No database with name {} exists", dbName));
        }
        return lmdbDb.getEntryCount();
    }


    @Override
    public List<RefStoreEntry> list(final int limit) {
        return list(limit, refStoreEntry -> true);
    }

    public List<RefStoreEntry> list(final int limit,
                                    final Predicate<RefStoreEntry> filter) {

        final List<RefStoreEntry> entries = new ArrayList<>();


        // TODO @AT This is all VERY crude. We should only be hitting the other DBs if we are returning
        //   a field from them or filtering on one of their fields. Also we should not be scanning over the
        //   whole of the kv/rv DBs, instead using start/stop keys built from the query expression.

        // TODO @AT This is not ideal holding a txn open for the whole query (if the query takes a long time)
        //   as read txns prevent writers from writing to reclaimed space in the db, so the store can get quite big.
        //   see https://lmdb.readthedocs.io/en/release/#transaction-management

        LmdbUtils.doWithReadTxn(lmdbEnvironment, readTxn -> {
            // Get all the KeyValue entries
            final List<RefStoreEntry> kvEntries = keyValueStoreDb.streamEntries(
                    readTxn,
                    KeyRange.all(),
                    entryStream -> entryStream
                            .map(entry -> {
                                final KeyValueStoreKey keyValueStoreKey = entry._1();
                                final ValueStoreKey valueStoreKey = entry._2();
                                final String keyStr = keyValueStoreKey.getKey();
                                return buildEntry(readTxn, keyValueStoreKey.getMapUid(), keyStr, valueStoreKey);
                            })
                            .filter(filter)
                            .limit(limit)
                            .collect(Collectors.toList()));
            entries.addAll(kvEntries);

            // If we haven't blown our limit then get the range entries
            final int rangeEntriesLimit = Math.min(limit, limit - entries.size());

            // Get all the KeyRangeValue entries
            final List<RefStoreEntry> rangeEntries = rangeStoreDb.streamEntries(
                    readTxn,
                    KeyRange.all(),
                    entryStream -> entryStream
                            .limit(rangeEntriesLimit)
                            .map(entry-> {
                                final RangeStoreKey rangeStoreKey = entry._1();
                                final ValueStoreKey valueStoreKey = entry._2();
                                final String keyStr = rangeStoreKey.getKeyRange().getFrom() + "-"
                                        + rangeStoreKey.getKeyRange().getTo();
                                return buildEntry(readTxn, rangeStoreKey.getMapUid(), keyStr, valueStoreKey);
                            })
                            .collect(Collectors.toList()));
            entries.addAll(rangeEntries);
        });

        return entries;
    }

    private RefStoreEntry buildEntry(final Txn<ByteBuffer> readTxn,
                                     final UID mapUid,
                                     final String key,
                                     final ValueStoreKey valueStoreKey) {

        final MapDefinition mapDefinition = mapDefinitionUIDStore.get(readTxn, mapUid)
                .orElseThrow(() -> new RuntimeException("No MapDefinition for UID " + mapUid.toString()));

        final RefDataProcessingInfo refDataProcessingInfo = processingInfoDb.get(readTxn, mapDefinition.getRefStreamDefinition())
                .orElse(null);

        final String value = getReferenceDataValue(readTxn, key, valueStoreKey);

        // Not ideal having to re-serialise the key
        final int valueReferenceCount = valueStore.getReferenceCount(readTxn, valueStoreKey)
                .orElse(-1);

        return new RefStoreEntry(mapDefinition, key, value, valueReferenceCount, refDataProcessingInfo);
    }

    private String getReferenceDataValue(final Txn<ByteBuffer> readTxn,
                                         final String key,
                                         final ValueStoreKey valueStoreKey) {
        try {
            // As RefDataValue is just wrapping a buffer for the xml values, which is tied to the LMDB cursor
            // these two lines need to be done with nothing in between that would alter the cursor position
            final RefDataValue refDataValue = valueStore.get(readTxn, valueStoreKey)
                    .orElse(null);

            return refDataValueConverter.refDataValueToString(refDataValue);
        } catch (Exception e) {
            LOGGER.error("Error getting value for key " + key, e);

            // Return a value so the once bad value doesn't break the whole resultset
            return "[ERROR: " + e.getMessage() + "]";
        }
    }

//    private Instant getPurgeCutOffEpoch(final StroomDuration purgeAge) {
//        return Instant.now().minus(purgeAge.getDuration());
//    }

//    private Instant getPurgeCutOffEpoch(final Instant now, final StroomDuration purgeAgeMs) {
//        return now.minus(purgeAgeMs.getDuration());
//    }

    private PooledByteBuffer getAccessTimeCutOffBuffer(final Instant now,
                                                       final StroomDuration purgeAge) {

        final Instant purgeCutOff = TimeUtils.durationToThreshold(now, purgeAge);

        LOGGER.info("Purging reference data store with purge age {} ({}), cut off time {}",
                purgeAge,
                purgeAge.getDuration(),
                purgeCutOff);

        final PooledByteBuffer pooledByteBuffer = byteBufferPool.getPooledByteBuffer(Long.BYTES);
        pooledByteBuffer.getByteBuffer().putLong(purgeCutOff.toEpochMilli());
        pooledByteBuffer.getByteBuffer().flip();
        return pooledByteBuffer;
    }

    @Override
    public SystemInfoResult getSystemInfo() {
        try {
            Tuple2<Optional<Instant>, Optional<Instant>> lastAccessedTimeRange = processingInfoDb.getLastAccessedTimeRange();

            SystemInfoResult.Builder builder = SystemInfoResult.builder(getSystemInfoName())
                    .withDetail("Path", dbDir.toAbsolutePath().toString())
                    .withDetail("Environment max size", maxSize)
                    .withDetail("Environment current size", ModelStringUtil.formatIECByteSizeString(getEnvironmentDiskUsage()))
                    .withDetail("Purge age", referenceDataConfig.getPurgeAge())
                    .withDetail("Purge cut off", TimeUtils.durationToThreshold(referenceDataConfig.getPurgeAge()).toString())
                    .withDetail("Max readers", maxReaders)
                    .withDetail("Read-ahead enabled", referenceDataConfig.isReadAheadEnabled())
                    .withDetail("Current buffer pool size", byteBufferPool.getCurrentPoolSize())
                    .withDetail("Earliest lastAccessedTime", lastAccessedTimeRange._1()
                            .map(Instant::toString)
                            .orElse(null))
                    .withDetail("Latest lastAccessedTime", lastAccessedTimeRange._2()
                            .map(Instant::toString)
                            .orElse(null));

            LmdbUtils.doWithReadTxn(lmdbEnvironment, txn -> {
                builder.withDetail("Database entry counts", databaseMap.entrySet().stream()
                        .collect(HasHealthCheck.buildTreeMapCollector(
                                Map.Entry::getKey,
                                entry -> entry.getValue().getEntryCount(txn))));
            });
            return builder.build();
        } catch (RuntimeException e) {
            return SystemInfoResult.builder(getSystemInfoName())
                    .withError(e)
                    .build();
        }
    }

    private long getEnvironmentDiskUsage() {
        long totalSizeBytes;
        try (final Stream<Path> fileStream = Files.list(dbDir)) {
            totalSizeBytes = fileStream
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .sum();
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Error calculating disk usage for path {}", dbDir.toAbsolutePath().toString(), e);
            totalSizeBytes = -1;
        }
        return totalSizeBytes;
    }

    private Path getStoreDir() {
        String storeDirStr = pathCreator.replaceSystemProperties(referenceDataConfig.getLocalDir());
        Path storeDir;
        if (storeDirStr == null) {
            LOGGER.info("Off heap store dir is not set, falling back to {}", tempDirProvider.get());
            storeDir = tempDirProvider.get();
            Objects.requireNonNull(storeDir, "Temp dir is not set");
            storeDir = storeDir.resolve(DEFAULT_STORE_SUB_DIR_NAME);
        } else {
            storeDirStr = pathCreator.replaceSystemProperties(storeDirStr);
            storeDir = Paths.get(storeDirStr);
        }

        try {
            LOGGER.debug("Ensuring directory {}", storeDir);
            Files.createDirectories(storeDir);
        } catch (IOException e) {
            throw new RuntimeException(LogUtil.message("Error ensuring store directory {} exists", storeDirStr), e);
        }

        return storeDir;
    }

    private static final class PurgeCounts {
        final int refStreamDefsDeletedCount;
        final RefStreamPurgeCounts refStreamPurgeCounts;

        private PurgeCounts(final int refStreamDefsDeletedCount,
                            final RefStreamPurgeCounts refStreamPurgeCounts) {
            this.refStreamDefsDeletedCount = refStreamDefsDeletedCount;
            this.refStreamPurgeCounts = refStreamPurgeCounts;
        }


        public static PurgeCounts zero() {
            return new PurgeCounts(0, RefStreamPurgeCounts.zero());
        }

        public PurgeCounts increment(final RefStreamPurgeCounts refStreamPurgeCounts) {
            return new PurgeCounts(
                    refStreamDefsDeletedCount + 1,
                    this.refStreamPurgeCounts.add(refStreamPurgeCounts));
        }
    }

    private static final class RefStreamPurgeCounts {
        final int mapsDeletedCount;
        final int valuesDeletedCount;
        final int valuesDeReferencedCount;

        private RefStreamPurgeCounts(final int mapsDeletedCount,
                                     final int valuesDeletedCount,
                                     final int valuesDeReferencedCount) {
            this.mapsDeletedCount = mapsDeletedCount;
            this.valuesDeletedCount = valuesDeletedCount;
            this.valuesDeReferencedCount = valuesDeReferencedCount;
        }

        public static RefStreamPurgeCounts zero() {
            return new RefStreamPurgeCounts(
                    0,
                    0,
                    0);
        }
        public RefStreamPurgeCounts add(final RefStreamPurgeCounts other) {
            return increment(
                    other.mapsDeletedCount,
                    other.valuesDeletedCount,
                    other.valuesDeReferencedCount
            );
        }

        public RefStreamPurgeCounts increment(final int mapsDeletedDelta,
                                              final int valuesDeletedDelta,
                                              final int valuesDeReferencedDelta) {
            return new RefStreamPurgeCounts(
                    mapsDeletedCount + mapsDeletedDelta,
                    valuesDeletedCount + valuesDeletedDelta,
                    valuesDeReferencedCount + valuesDeReferencedDelta);
        }
    }
}
