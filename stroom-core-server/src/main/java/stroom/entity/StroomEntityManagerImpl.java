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

package stroom.entity;

import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.entity.shared.EntityEvent;
import stroom.entity.shared.EntityEventBus;
import stroom.entity.shared.AuditedEntity;
import stroom.util.shared.BaseCriteria;
import stroom.entity.shared.BaseEntity;
import stroom.util.shared.BaseResultList;
import stroom.entity.shared.DocRefUtil;
import stroom.entity.shared.Entity;
import stroom.entity.shared.EntityAction;
import stroom.util.shared.SummaryDataRow;
import stroom.entity.util.AbstractSqlBuilder;
import stroom.entity.util.EntityServiceLogUtil;
import stroom.entity.util.HqlBuilder;
import stroom.entity.util.SqlBuilder;
import stroom.entity.util.SqlUtil;
import stroom.persist.EntityManagerSupport;
import stroom.security.SecurityContext;
import stroom.util.logging.LogExecutionTime;
import stroom.util.shared.EqualsUtil;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StroomEntityManagerImpl implements StroomEntityManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StroomEntityManagerImpl.class);

    private final EntityManagerSupport entityManagerSupport;
    private final Provider<EntityEventBus> entityEventBusProvider;
    private final Provider<SecurityContext> securityContextProvider;

    @Inject
    StroomEntityManagerImpl(final EntityManagerSupport entityManagerSupport,
                            final Provider<EntityEventBus> entityEventBusProvider,
                            final Provider<SecurityContext> securityContextProvider) {
        this.entityManagerSupport = entityManagerSupport;
        this.entityEventBusProvider = entityEventBusProvider;
        this.securityContextProvider = securityContextProvider;
    }

    private String getCurrentUser() {
        try {
            final SecurityContext securityContext = securityContextProvider.get();
            if (securityContext != null) {
                return securityContext.getUserId();
            }
        } catch (final RuntimeException e) {
            LOGGER.debug(e.getMessage(), e);
        }

        return null;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T extends Entity> T loadEntity(final Class<?> clazz, final T entity) {
        if (entity == null) {
            return null;
        }
        return (T) entityManagerSupport.executeResult(entityManager -> entityManager.find(clazz, entity.getPrimaryKey()));
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T extends Entity> T loadEntityById(final Class<?> clazz, final long id) {
        return (T) entityManagerSupport.executeResult(entityManager -> entityManager.find(clazz, id));
    }

    @Override
    public <T extends Entity> T saveEntity(final T entity) {
        return entityManagerSupport.transactionResult(entityManager -> internalSaveEntity(entity, entityManager, true));
    }

    private <T extends Entity> T internalSaveEntity(final T entity, final EntityManager entityManager, final boolean performChecksAndEvents) {
        if (entity instanceof AuditedEntity) {
            final AuditedEntity auditedEntity = (AuditedEntity) entity;
            final long now = System.currentTimeMillis();
            if (!entity.isPersistent()) {
                auditedEntity.setCreateTime(now);
                auditedEntity.setCreateUser(getCurrentUser());
            }
            auditedEntity.setUpdateTime(now);
            auditedEntity.setUpdateUser(getCurrentUser());
        }

        // Update?
        if (entity.isPersistent()) {
            final Object originalKey = entity.getPrimaryKey();
            replaceLazyObjects(entityManager, entity, new HashSet<>());
            final T updatedEntity = entityManager.merge(entity);
            // Here if we think we are persistent and we get a new id somebody
            // must have deleted us and the entity manager is just allocating us
            // again .... roll back !
            if (!EqualsUtil.isEquals(updatedEntity.getPrimaryKey(), originalKey)) {
                final String message = "Attempt to update deleted entity of type " + entity.getType() + " (key="
                        + originalKey + ", newKey=" + updatedEntity.getPrimaryKey() + ")";
                LOGGER.debug(message);
                throw new OptimisticLockException(message, null, entity);
            }

            if (performChecksAndEvents) {
                EntityEvent.fire(entityEventBusProvider.get(), DocRefUtil.create(updatedEntity), EntityAction.UPDATE);
            }

            return updatedEntity;
        }

        entityManager.persist(entity);

        // Create the new keys
        entityManager.flush();

        if (performChecksAndEvents) {
            EntityEvent.fire(entityEventBusProvider.get(), DocRefUtil.create(entity), EntityAction.CREATE);
        }

        return entity;
    }

    @Override
    public Long executeNativeUpdate(final SqlBuilder sql) {
        return entityManagerSupport.transactionResult(entityManager -> {
            final LogExecutionTime logExecutionTime = new LogExecutionTime();
            Long rtn;
            try {
                final Query query = createNativeQuery(entityManager, sql.toString());
                SqlUtil.setParameters(query, sql);
                rtn = (long) query.executeUpdate();
                EntityServiceLogUtil.logUpdate(LOGGER, "executeNativeUpdate", logExecutionTime, rtn, sql);
            } catch (final RuntimeException e) {
                LOGGER.debug("executeNativeUpdate - " + sql.toString() + "\"" + sql.toTraceString() + "\"", e);
                throw e;
            }
            return rtn;
        });
    }

    @Override
    public long executeNativeQueryLongResult(final SqlBuilder sql) {
        return executeQueryLongResult(true, sql);
    }

    @Override
    public long executeQueryLongResult(final HqlBuilder sql) {
        return executeQueryLongResult(false, sql);
    }

    private long executeQueryLongResult(final boolean isNative, final AbstractSqlBuilder sql) {
        return entityManagerSupport.executeResult(entityManager -> {
            final LogExecutionTime logExecutionTime = new LogExecutionTime();
            long rtn = 0;
            try {
                Query query;
                if (isNative) {
                    query = createNativeQuery(entityManager, sql.toString());
                } else {
                    query = createQuery(entityManager, sql.toString());
                }
                SqlUtil.setParameters(query, sql);
                final List<?> list = query.getResultList();
                if (list != null && list.size() > 0) {
                    final Object row = list.get(0);
                    if (row instanceof Object[]) {
                        final Object item = ((Object[]) row)[0];
                        if (item instanceof Number) {
                            rtn = ((Number) item).longValue();
                        }
                    }
                    if (row instanceof Number) {
                        rtn = ((Number) row).longValue();
                    }
                }
                EntityServiceLogUtil.logUpdate(LOGGER, "executeQueryLongResult", logExecutionTime, rtn, sql);
            } catch (final RuntimeException e) {
                LOGGER.debug("executeQueryLongResult - {}", sql, e);
                throw e;
            }
            return rtn;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public BaseResultList<SummaryDataRow> executeNativeQuerySummaryDataResult(final SqlBuilder sql,
                                                                              final int numberKeys) {
        final ArrayList<SummaryDataRow> summaryData = new ArrayList<>();

        final List<Object[]> list = executeNativeQueryResultList(sql);

        for (final Object[] row : list) {
            final SummaryDataRow summaryDataRow = new SummaryDataRow();
            int pos = 0;
            for (int i = 0; i < numberKeys; i++) {
                final Object key = row[pos++];
                final Object value = row[pos++];
                summaryDataRow.getKey().add(((Number) key).longValue());
                summaryDataRow.getLabel().add((String.valueOf(value)));
            }
            summaryDataRow.setCount(((Number) row[pos++]).longValue());
            summaryData.add(summaryDataRow);
        }

        return BaseResultList.createUnboundedList(summaryData);
    }

    @Override
    public <T extends Entity> Boolean deleteEntity(final T entity) {
        // Check for locking and make sure the entity is attached before we try
        // and delete it.
        return entityManagerSupport.transactionResult(entityManager -> {
            final Entity dbEntity = internalSaveEntity(entity, entityManager, false);
            entityManager.remove(dbEntity);
            EntityEvent.fire(entityEventBusProvider.get(), DocRefUtil.create(dbEntity), EntityAction.DELETE);
            return Boolean.TRUE;
        });
    }

    @Override
    public <T extends Entity> void detach(final T entity) {
        entityManagerSupport.execute(entityManager -> {
            if (entityManager.contains(entity)) {
                entityManager.detach(entity);
            }
        });
    }

    @Override
    public void flush() {
        entityManagerSupport.execute(EntityManager::flush);
    }

//    @Override
//    public void shutdown() {
//        // Shut down the database if we are using Hypersonic
//        if (!stroomDatabaseInfoProvider.get().isMysql()) {
//            final SqlBuilder sql = new SqlBuilder();
//            sql.append("shutdown");
//            executeNativeUpdate(sql);
//        }
//    }

    @SuppressWarnings("rawtypes")
    @Override
    public String runSubSelectQuery(final HqlBuilder sql, final boolean handleNull) {
        final List results = executeQueryResultList(sql);
        final StringBuilder subSet = new StringBuilder();
        boolean doneOne = false;
        for (final Object row : results) {
            if (doneOne) {
                subSet.append(",");
            }
            if (row != null) {
                if (!doneOne) {
                    subSet.append(" IN (");
                }
                subSet.append(row);
                doneOne = true;
            }
        }

        if (doneOne) {
            subSet.append(")");
            return subSet.toString();
        } else {
            if (handleNull) {
                return " IS NULL";
            } else {
                return null;
            }
        }

    }

    @Override
    public boolean hasNativeColumn(final String nativeTable, final String nativeColumn) {
        final SqlBuilder sql = new SqlBuilder();
        sql.append("select column_name, table_name from information_schema.columns where table_name = ");
        sql.arg(nativeTable);
        sql.append(" and column_name = ");
        sql.arg(nativeColumn);
        sql.append(" and table_schema = ");
        sql.arg("stroom");
        final List<?> rows = executeNativeQueryResultList(sql, null);
        return rows != null && rows.size() > 0;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List executeQueryResultList(final HqlBuilder sql) {
        return executeQueryResultList(sql, null, false);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List executeQueryResultList(final HqlBuilder sql, final BaseCriteria criteria) {
        return executeQueryResultList(sql, criteria, false);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List executeQueryResultList(final HqlBuilder sql, final BaseCriteria criteria, final boolean allowCaching) {
        return entityManagerSupport.executeResult(entityManager -> {
            final LogExecutionTime logExecutionTime = new LogExecutionTime();
            List rtn;

            try {
                final Query query = createQuery(entityManager, sql.toString());
                SqlUtil.setParameters(query, sql);
                if (criteria != null) {
                    sql.applyRestrictionCriteria(query, criteria);
                }
                rtn = query.getResultList();
                EntityServiceLogUtil.logQuery(LOGGER, "executeQueryResultList", logExecutionTime, rtn, sql);

            } catch (final RuntimeException e) {
                LOGGER.debug("executeQueryResultList() - {}", sql, e);
                throw e;
            }
            return rtn;
        });
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List executeNativeQueryResultList(final SqlBuilder sql) {
        return entityManagerSupport.executeResult(entityManager -> {
            final LogExecutionTime logExecutionTime = new LogExecutionTime();
            List rtn;
            try {
                final Query query = createNativeQuery(entityManager, sql.toString());
                SqlUtil.setParameters(query, sql);
                rtn = query.getResultList();
                EntityServiceLogUtil.logQuery(LOGGER, "executeQueryResultList", logExecutionTime, rtn, sql);

            } catch (final RuntimeException e) {
                LOGGER.debug("executeQueryResultList() - {}", sql, e);
                throw e;
            }
            return rtn;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> executeNativeQueryResultList(final SqlBuilder sql, final Class<?> clazz) {
        return entityManagerSupport.executeResult(entityManager -> {
            final LogExecutionTime logExecutionTime = new LogExecutionTime();
            List<T> rtn;
            try {
                final Query query = createNativeQuery(entityManager, sql.toString(), clazz);
                SqlUtil.setParameters(query, sql);
                rtn = query.getResultList();
                EntityServiceLogUtil.logQuery(LOGGER, "executeNativeQueryResultList", logExecutionTime, rtn, sql);

            } catch (final RuntimeException e) {
                LOGGER.debug("executeNativeQueryResultList() - {}", sql, e);
                throw e;
            }
            return rtn;
        });
    }

    private Query createNativeQuery(final EntityManager entityManager, final String sql, final Class<?> clazz) {
        return entityManager.createNativeQuery(sql, clazz);
    }

    private Query createNativeQuery(final EntityManager entityManager, final String sql) {
        return entityManager.createNativeQuery(sql);
    }

    private Query createQuery(final EntityManager entityManager, final String sql) {
        return entityManager.createQuery(sql);
    }

//    @Override
//    public void setFlushMode(final EntityManager entityManager, final FlushModeType mode) {
//        entityManager.setFlushMode(mode);
//    }

    private void replaceLazyObjects(final EntityManager entityManager, final Entity entity, final Set<Entity> processedSet) {
        if (processedSet.contains(entity)) {
            return;
        }
        processedSet.add(entity);
        try {
            final PropertyDescriptor[] fields = Introspector.getBeanInfo(entity.getClass()).getPropertyDescriptors();
            for (final PropertyDescriptor field : fields) {
                // We can only do anything with readable properties
                if (field.getReadMethod() != null && field.getReadMethod().getParameterTypes().length == 0) {
                    // Only process writable properties
                    if (field.getWriteMethod() != null) {
                        final Object oldValue = field.getReadMethod().invoke(entity);
                        if (oldValue instanceof BaseEntity && !(oldValue instanceof HibernateProxy)) {
                            final BaseEntity be = (BaseEntity) oldValue;
                            if (be.isPersistent()) {
                                if (be.isStub()) {
                                    // One of our mocked up objects with just
                                    // the key set
                                    field.getWriteMethod().invoke(entity, entityManager.getReference(be.getClass(), be.getId()));
                                } else {
                                    // A real object not proxied... we should
                                    // walk it to look for other lazy objects.
                                    replaceLazyObjects(entityManager, be, processedSet);
                                }

                            }
                        }
                    }
                }
            }
        } catch (IllegalArgumentException | InvocationTargetException | IntrospectionException
                | IllegalAccessException irEx) {
            throw new RuntimeException(irEx);
        }
    }

    @Override
    public void clearContext() {
        entityManagerSupport.transaction(EntityManager::clear);
    }
}
