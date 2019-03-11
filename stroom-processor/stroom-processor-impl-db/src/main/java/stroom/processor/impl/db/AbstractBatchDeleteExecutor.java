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

package stroom.processor.impl.db;

import org.jooq.Field;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.cluster.lock.api.ClusterLockService;
import stroom.processor.impl.BatchDeleteConfig;
import stroom.task.api.TaskContext;
import stroom.util.logging.LogExecutionTime;
import stroom.util.shared.ModelStringUtil;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractBatchDeleteExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBatchDeleteExecutor.class);

    private BatchIdTransactionHelper batchIdTransactionHelper;
    private final ClusterLockService clusterLockService;
    private final TaskContext taskContext;

    private final String taskName;
    private final String clusterLockName;
    private final BatchDeleteConfig batchDeleteConfig;
    private final String tempIdTable;

    public AbstractBatchDeleteExecutor(final ClusterLockService clusterLockService,
                                       final TaskContext taskContext,
                                       final String taskName,
                                       final String clusterLockName,
                                       final BatchDeleteConfig batchDeleteConfig,
                                       final String tempIdTable) {
        this.clusterLockService = clusterLockService;
        this.taskContext = taskContext;

        this.taskName = taskName;
        this.clusterLockName = clusterLockName;
        this.batchDeleteConfig = batchDeleteConfig;
        this.tempIdTable = tempIdTable;
    }

    public void setBatchIdTransactionHelper(final BatchIdTransactionHelper batchIdTransactionHelper) {
        this.batchIdTransactionHelper = batchIdTransactionHelper;
    }

    final void lockAndDelete() {
        LOGGER.info(taskName + " - start");
        clusterLockService.tryLock(clusterLockName, () -> {
            try {
                if (!Thread.currentThread().isInterrupted()) {
                    final LogExecutionTime logExecutionTime = new LogExecutionTime();
                    final long age = getDeleteAge(batchDeleteConfig);
                    if (age > 0) {
                        delete(age);
                    }
                    LOGGER.info(taskName + " - finished in {}", logExecutionTime);
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }

    public void delete(final long age) {
        if (!Thread.currentThread().isInterrupted()) {
            long count = 0;
            long total = 0;

            final LogExecutionTime logExecutionTime = new LogExecutionTime();

            final int deleteBatchSize = batchDeleteConfig.getDeleteBatchSize();

            // Ensure the temp id table exists.
            createTempIdTable();

            // See if there are ids in the table already. There shouldn't be if
            // the task completed successfully last time.
            count = getIdCount(total);
            if (count > 0) {
                LOGGER.warn("{} ids found from previous delete that must not have completed successfully", count);
                // Try and delete the remaining batch.
                total += count;
                deleteCurrentBatch(total);
                // Remove the current batch of ids from the id table.
                truncateTempIdTable(total);
            }

            if (!Thread.currentThread().isInterrupted()) {
                do {
                    // Insert a batch of ids into the temp id table and find out
                    // how many were inserted.
                    count = insertIntoTempIdTable(age, deleteBatchSize, total);

                    // If we inserted some ids then try and delete this batch.
                    if (count > 0) {
                        total += count;
                        deleteCurrentBatch(total);
                        // Remove the current batch of ids from the id table.
                        truncateTempIdTable(total);
                    }
                } while (!Thread.currentThread().isInterrupted() && count >= deleteBatchSize);
            }

            LOGGER.debug("Deleted {} streams in {}.", total, logExecutionTime);
        }
    }

    protected abstract void deleteCurrentBatch(final long total);

    private void createTempIdTable() {
        info("Creating temp id table");
        final LogExecutionTime logExecutionTime = new LogExecutionTime();
        batchIdTransactionHelper.createTempIdTable(tempIdTable);
        LOGGER.debug("Created temp id table in {}", logExecutionTime);
    }

    private long getIdCount(final long total) {
        info("Getting id count (total={})", total);
        final LogExecutionTime logExecutionTime = new LogExecutionTime();
        final long count = batchIdTransactionHelper.getTempIdCount(tempIdTable);
        LOGGER.debug("Got {} ids in {}", count, logExecutionTime);
        return count;
    }

    private long insertIntoTempIdTable(final long age, final int batchSize, final long total) {
        info("Inserting ids for deletion into temp id table (total={})", total);
        final LogExecutionTime logExecutionTime = new LogExecutionTime();
        final List<Long> idList = getDeleteIdList(age, batchSize);
        final long count = batchIdTransactionHelper.insertIntoTempIdTable(idList);
        LOGGER.debug("Inserted {} ids in {}", count, logExecutionTime);
        return count;
    }

    protected abstract List getDeleteIdList(final long age, final int batchSize);

    protected final void deleteWithJoin(final Table<?> fromTable, final Field<Long> fromColumn, final String type,
                                        final long total) {
        info("Deleting {} (total={})", type, total);
        final LogExecutionTime logExecutionTime = new LogExecutionTime();

        // TODO : @66 REMOVE JOIN TO STREAM TABLE.

        final long count = batchIdTransactionHelper.deleteWithJoin(fromTable, fromColumn);
        LOGGER.debug("Deleted {} {} in {}", new Object[]{count, type, logExecutionTime});
    }

    private void truncateTempIdTable(final long total) {
        info("Truncating temp id table (total={})", total);
        final LogExecutionTime logExecutionTime = new LogExecutionTime();
        batchIdTransactionHelper.truncateTempIdTable(tempIdTable);
        LOGGER.debug("Truncated temp id table in {}", logExecutionTime);
    }

    private void info(final Object... args) {
        taskContext.info(args);
        Arrays.asList(args).forEach(arg -> LOGGER.debug(arg.toString()));
    }

    private Long getDeleteAge(final BatchDeleteConfig config) {
        Long age = null;
        final String durationString = config.getDeletePurgeAge();
        if (durationString != null && !durationString.isEmpty()) {
            try {
                final long duration = ModelStringUtil.parseDurationString(durationString);
                age = System.currentTimeMillis() - duration;
            } catch (final RuntimeException e) {
                LOGGER.error("Error reading config");
            }
        }
        return age;
    }
}
