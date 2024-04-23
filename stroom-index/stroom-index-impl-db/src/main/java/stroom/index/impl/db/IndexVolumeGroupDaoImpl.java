package stroom.index.impl.db;

import stroom.db.util.GenericDao;
import stroom.db.util.JooqUtil;
import stroom.db.util.JooqUtil.BooleanOperator;
import stroom.docref.DocRef;
import stroom.index.impl.IndexStore;
import stroom.index.impl.IndexVolumeGroupDao;
import stroom.index.impl.db.jooq.tables.records.IndexVolumeGroupRecord;
import stroom.index.shared.IndexVolumeGroup;
import stroom.util.NullSafe;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import static stroom.index.impl.db.jooq.Tables.INDEX_VOLUME_GROUP;
import static stroom.index.impl.db.jooq.tables.IndexVolume.INDEX_VOLUME;

class IndexVolumeGroupDaoImpl implements IndexVolumeGroupDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexVolumeGroupDaoImpl.class);

    static final Function<Record, IndexVolumeGroup> RECORD_TO_INDEX_VOLUME_GROUP_MAPPER = record -> {
        final IndexVolumeGroup indexVolumeGroup = new IndexVolumeGroup();
        indexVolumeGroup.setId(record.get(INDEX_VOLUME_GROUP.ID));
        indexVolumeGroup.setVersion(record.get(INDEX_VOLUME_GROUP.VERSION));
        indexVolumeGroup.setCreateTimeMs(record.get(INDEX_VOLUME_GROUP.CREATE_TIME_MS));
        indexVolumeGroup.setCreateUser(record.get(INDEX_VOLUME_GROUP.CREATE_USER));
        indexVolumeGroup.setUpdateTimeMs(record.get(INDEX_VOLUME_GROUP.UPDATE_TIME_MS));
        indexVolumeGroup.setUpdateUser(record.get(INDEX_VOLUME_GROUP.UPDATE_USER));
        indexVolumeGroup.setName(record.get(INDEX_VOLUME_GROUP.NAME));
        indexVolumeGroup.setUuid(record.get(INDEX_VOLUME_GROUP.UUID));
        indexVolumeGroup.setDefaultVolume(fromDbIsDefaultValue(record.get(INDEX_VOLUME_GROUP.IS_DEFAULT)));
        return indexVolumeGroup;
    };

    @SuppressWarnings("checkstyle:LineLength")
    private static final BiFunction<IndexVolumeGroup, IndexVolumeGroupRecord, IndexVolumeGroupRecord> INDEX_VOLUME_GROUP_TO_RECORD_MAPPER =
            (indexVolumeGroup, record) -> {
                record.from(indexVolumeGroup);
                record.set(INDEX_VOLUME_GROUP.ID, indexVolumeGroup.getId());
                record.set(INDEX_VOLUME_GROUP.VERSION, indexVolumeGroup.getVersion());
                record.set(INDEX_VOLUME_GROUP.CREATE_TIME_MS, indexVolumeGroup.getCreateTimeMs());
                record.set(INDEX_VOLUME_GROUP.CREATE_USER, indexVolumeGroup.getCreateUser());
                record.set(INDEX_VOLUME_GROUP.UPDATE_TIME_MS, indexVolumeGroup.getUpdateTimeMs());
                record.set(INDEX_VOLUME_GROUP.UPDATE_USER, indexVolumeGroup.getUpdateUser());
                record.set(INDEX_VOLUME_GROUP.NAME, indexVolumeGroup.getName());
                record.set(INDEX_VOLUME_GROUP.UUID, indexVolumeGroup.getUuid());
                record.set(INDEX_VOLUME_GROUP.IS_DEFAULT, toDbIsDefaultValue(indexVolumeGroup));
                return record;
            };

    private final IndexDbConnProvider indexDbConnProvider;
    private final Provider<IndexStore> indexStoreProvider;
    private final GenericDao<IndexVolumeGroupRecord, IndexVolumeGroup, Integer> genericDao;

    @Inject
    IndexVolumeGroupDaoImpl(final IndexDbConnProvider indexDbConnProvider,
                            final Provider<IndexStore> indexStoreProvider) {
        this.indexDbConnProvider = indexDbConnProvider;
        this.indexStoreProvider = indexStoreProvider;
        genericDao = new GenericDao<>(
                indexDbConnProvider,
                INDEX_VOLUME_GROUP,
                INDEX_VOLUME_GROUP.ID,
                INDEX_VOLUME_GROUP_TO_RECORD_MAPPER,
                RECORD_TO_INDEX_VOLUME_GROUP_MAPPER);
    }

    private void removeCurrentDefault(final DSLContext context) {
        context.update(INDEX_VOLUME_GROUP)
                .set(INDEX_VOLUME_GROUP.IS_DEFAULT, (Boolean) null)
                .where(INDEX_VOLUME_GROUP.IS_DEFAULT.eq(true))
                .execute();
    }

    @Override
    public IndexVolumeGroup create(final IndexVolumeGroup indexVolumeGroup) {
        Objects.requireNonNull(indexVolumeGroup);
        return JooqUtil.transactionResultWithOptimisticLocking(indexDbConnProvider, context -> {
            if (indexVolumeGroup.isDefaultVolume()) {
                // Can only have one that is default
                removeCurrentDefault(context);
            }
            return genericDao.create(context, indexVolumeGroup);
        });
    }

    @Override
    public IndexVolumeGroup getOrCreate(final IndexVolumeGroup indexVolumeGroup) {
        Objects.requireNonNull(indexVolumeGroup);
        return JooqUtil.transactionResultWithOptimisticLocking(indexDbConnProvider, context -> {
            if (indexVolumeGroup.isDefaultVolume()) {
                // Can only have one that is default so ensure all others are not
                context.update(INDEX_VOLUME_GROUP)
                        .set(INDEX_VOLUME_GROUP.IS_DEFAULT, (Boolean) null)
                        .where(INDEX_VOLUME_GROUP.IS_DEFAULT.eq(true))
                        .and(INDEX_VOLUME_GROUP.UUID.ne(indexVolumeGroup.getUuid()))
                        .execute();
            }
            return genericDao.tryCreate(
                    context,
                    indexVolumeGroup,
                    INDEX_VOLUME_GROUP.UUID,
                    null,
                    null);
        });
    }

    @Override
    public IndexVolumeGroup update(IndexVolumeGroup indexVolumeGroup) {
        return JooqUtil.transactionResult(indexDbConnProvider, context -> {
            final IndexVolumeGroup saved;
            try {
                if (indexVolumeGroup.isDefaultVolume()) {
                    // Can only have one that is default
                    removeCurrentDefault(context);
                }
                saved = genericDao.update(context, indexVolumeGroup);
            } catch (DataAccessException e) {
                if (e.getCause() != null
                        && e.getCause() instanceof SQLIntegrityConstraintViolationException) {
                    final var sqlEx = (SQLIntegrityConstraintViolationException) e.getCause();
                    if (sqlEx.getErrorCode() == 1062
                            && sqlEx.getMessage().contains("Duplicate entry")
                            && sqlEx.getMessage().contains("key")
                            && sqlEx.getMessage().contains(INDEX_VOLUME_GROUP.NAME.getName())) {
                        throw new RuntimeException("An index volume group already exists with name '"
                                + indexVolumeGroup.getName() + "'");
                    }
                }
                throw e;
            }

            // If the group name has changed then update indexes to point to the new group name.
//        if (currentGroupName != null && !currentGroupName.equals(saved.getName())) {
//            final IndexStore indexStore = indexStoreProvider.get();
//            if (indexStore != null) {
//                final List<DocRef> indexes = indexStore.list();
//                for (final DocRef docRef : indexes) {
//                    final IndexDoc indexDoc = indexStore.readDocument(docRef);
//                    if (indexDoc.getVolumeGroupName() != null &&
//                            indexDoc.getVolumeGroupName().equals(currentGroupName)) {
//                        indexDoc.setVolumeGroupName(saved.getName());
//                        LOGGER.info("Updating index {} ({}) to change volume group name from {} to {}",
//                                indexDoc.getName(),
//                                indexDoc.getUuid(),
//                                currentGroupName,
//                                saved.getName());
//                        indexStore.writeDocument(indexDoc);
//                    }
//                }
//            }
//        }

            return saved;
        });
    }

    @Override
    public IndexVolumeGroup get(final int id) {
        return JooqUtil.contextResult(indexDbConnProvider, context -> context
                        .select()
                        .from(INDEX_VOLUME_GROUP)
                        .where(INDEX_VOLUME_GROUP.ID.eq(id))
                        .fetchOptional())
                .map(RECORD_TO_INDEX_VOLUME_GROUP_MAPPER)
                .orElse(null);
    }

    @Override
    public IndexVolumeGroup get(final String name) {
        return JooqUtil.contextResult(indexDbConnProvider, context -> context
                        .select()
                        .from(INDEX_VOLUME_GROUP)
                        .where(INDEX_VOLUME_GROUP.NAME.eq(name))
                        .fetchOptional())
                .map(RECORD_TO_INDEX_VOLUME_GROUP_MAPPER)
                .orElse(null);
    }

    @Override
    public IndexVolumeGroup get(final DocRef docRef) {
        if (docRef != null) {
            Objects.requireNonNull(docRef.getUuid());
            return JooqUtil.contextResult(indexDbConnProvider, context -> context
                            .select()
                            .from(INDEX_VOLUME_GROUP)
                            .where(INDEX_VOLUME_GROUP.UUID.eq(docRef.getUuid()))
                            .fetchOptional())
                    .map(RECORD_TO_INDEX_VOLUME_GROUP_MAPPER)
                    .orElse(null);
        } else {
            return null;
        }
    }

    @Override
    public List<IndexVolumeGroup> find(final List<String> nameFilters, final boolean allowWildCards) {
        final Condition condition = JooqUtil.createWildCardedStringsCondition(
                INDEX_VOLUME_GROUP.NAME, nameFilters, true, BooleanOperator.OR);

        return JooqUtil.contextResult(indexDbConnProvider, context ->
                        context.select()
                                .from(INDEX_VOLUME_GROUP)
                                .where(condition)
                                .fetch())
                .stream()
                .map(RECORD_TO_INDEX_VOLUME_GROUP_MAPPER)
                .toList();
    }

    @Override
    public IndexVolumeGroup getDefaultVolumeGroup() {
        return JooqUtil.contextResult(indexDbConnProvider, context -> context
                        .select()
                        .from(INDEX_VOLUME_GROUP)
                        .where(INDEX_VOLUME_GROUP.IS_DEFAULT.eq(true))
                        .fetchOptional())
                .map(RECORD_TO_INDEX_VOLUME_GROUP_MAPPER)
                .orElse(null);
    }

    @Override
    public List<String> getNames() {
        return JooqUtil.contextResult(indexDbConnProvider, context -> context
                .select(INDEX_VOLUME_GROUP.NAME)
                .from(INDEX_VOLUME_GROUP)
                .orderBy(INDEX_VOLUME_GROUP.NAME)
                .fetch(INDEX_VOLUME_GROUP.NAME));
    }

    @Override
    public List<IndexVolumeGroup> getAll() {
        return JooqUtil.contextResult(indexDbConnProvider, context -> context
                        .select()
                        .from(INDEX_VOLUME_GROUP)
                        .orderBy(INDEX_VOLUME_GROUP.NAME)
                        .fetch())
                .map(RECORD_TO_INDEX_VOLUME_GROUP_MAPPER::apply);
    }

    @Override
    public void delete(final String name) {
        final var indexVolumeGroupToDelete = get(name);
        genericDao.delete(indexVolumeGroupToDelete.getId());
    }

    @Override
    public void delete(int id) {
        JooqUtil.transaction(indexDbConnProvider, txnContext -> {
            // Delete the child volumes first
            txnContext.deleteFrom(INDEX_VOLUME)
                    .where(INDEX_VOLUME.FK_INDEX_VOLUME_GROUP_ID.eq(id))
                    .execute();

            genericDao.delete(txnContext, id);
        });
    }

    private static Boolean toDbIsDefaultValue(final IndexVolumeGroup indexVolumeGroup) {
        return indexVolumeGroup.isDefaultVolume()
                ? Boolean.TRUE
                : null;
    }

    private static boolean fromDbIsDefaultValue(final Boolean isDefault) {
        return NullSafe.isTrue(isDefault);
    }

    private void setAllOthersNonDefault(final IndexVolumeGroup indexVolumeGroup,
                                        final DSLContext context) {
        context.update(INDEX_VOLUME_GROUP)
                .set(INDEX_VOLUME_GROUP.IS_DEFAULT, toDbIsDefaultValue(indexVolumeGroup))
                .where(INDEX_VOLUME_GROUP.ID.notEqual(indexVolumeGroup.getId()))
                .execute();
    }
}
