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

package stroom.statistics.impl.sql;

import org.apache.commons.lang3.mutable.MutableLong;
import stroom.security.api.SecurityContext;
import stroom.statistics.impl.sql.shared.StatisticType;
import stroom.task.api.AbstractTaskHandler;
import stroom.task.api.TaskContext;
import stroom.util.logging.LambdaLogUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogExecutionTime;
import stroom.util.shared.ModelStringUtil;
import stroom.task.api.VoidResult;

import javax.inject.Inject;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;


public class SQLStatisticFlushTaskHandler extends AbstractTaskHandler<SQLStatisticFlushTask, VoidResult> {
    /**
     * The number of records to flush to the DB in one go.
     */
    private static final int BATCH_SIZE = 5000;
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SQLStatisticFlushTaskHandler.class);
    private final SQLStatisticValueBatchSaveService sqlStatisticValueBatchSaveService;
    private final TaskContext taskContext;
    private final SecurityContext securityContext;

    private LogExecutionTime logExecutionTime;
    private int count;
    private int savedCount;
    private int total;

    @Inject
    public SQLStatisticFlushTaskHandler(final SQLStatisticValueBatchSaveService sqlStatisticValueBatchSaveService,
                                        final TaskContext taskContext,
                                        final SecurityContext securityContext) {
        this.sqlStatisticValueBatchSaveService = sqlStatisticValueBatchSaveService;
        this.taskContext = taskContext;
        this.securityContext = securityContext;
    }

    @Override
    public VoidResult exec(final SQLStatisticFlushTask task) {
        return securityContext.secureResult(() -> {
            flush(task.getMap());
            return new VoidResult();
        });
    }

    private void flush(final SQLStatisticAggregateMap map) {
        if (map != null) {
            logExecutionTime = new LogExecutionTime();
            count = 0;
            savedCount = 0;
            total = map.size();

            final int batchSizetoUse = BATCH_SIZE;

            final Supplier<String> messageSupplier = () -> "Flushing statistics (batch size=" + batchSizetoUse + ")";
            LOGGER.info(messageSupplier);
            taskContext.info(messageSupplier);

            final List<SQLStatisticValueSourceDO> batchInsert = new ArrayList<>();
            // Store all aggregated entries.
            for (final Entry<SQLStatKey, MutableLong> entry : map.countEntrySet()) {
                if (!Thread.currentThread().isInterrupted()) {
                    final long ms = entry.getKey().getMs();
                    final String name = entry.getKey().getName();
                    final long value = entry.getValue().longValue();

                    final SQLStatisticValueSourceDO insert = new SQLStatisticValueSourceDO();
                    insert.setCreateMs(ms);
                    insert.setName(name);
                    insert.setType(StatisticType.COUNT);
                    insert.setValue(value);

                    batchInsert.add(insert);

                    count++;

                    if (batchInsert.size() >= batchSizetoUse) {
                        doSaveBatch(batchInsert);
                    }
                }
            }
            for (final Entry<SQLStatKey, Double> entry : map.valueEntrySet()) {
                if (!Thread.currentThread().isInterrupted()) {
                    final long ms = entry.getKey().getMs();
                    final String name = entry.getKey().getName();
                    final long value = entry.getValue().longValue();

                    final SQLStatisticValueSourceDO insert = new SQLStatisticValueSourceDO();
                    insert.setCreateMs(ms);
                    insert.setName(name);
                    insert.setType(StatisticType.VALUE);
                    insert.setValue(value);

                    batchInsert.add(insert);

                    count++;

                    if (batchInsert.size() >= batchSizetoUse) {
                        doSaveBatch(batchInsert);
                    }
                }
            }

            if (!Thread.currentThread().isInterrupted()) {
                if (batchInsert.size() > 0) {
                    doSaveBatch(batchInsert);
                }
            }
        }
    }

    private void doSaveBatch(final List<SQLStatisticValueSourceDO> batchInsert) {
        try {
            final int seconds = (int) (logExecutionTime.getDuration() / 1000L);

            if (seconds > 0) {
                taskContext.info(LambdaLogUtil.message("Saving {}/{} ({} ps)", ModelStringUtil.formatCsv(count),
                        ModelStringUtil.formatCsv(total), ModelStringUtil.formatCsv(savedCount / seconds)));
            } else {
                taskContext.info(LambdaLogUtil.message("Saving {}/{} (? ps)", ModelStringUtil.formatCsv(count),
                        ModelStringUtil.formatCsv(total)));

            }

            sqlStatisticValueBatchSaveService.saveBatchStatisticValueSource_String(batchInsert);

            savedCount += batchInsert.size();
        } catch (final RuntimeException e) {
            LOGGER.debug(e::getMessage, e);
            LOGGER.warn(LambdaLogUtil.message("doSaveBatch() - Failed to insert {} records will try slower PreparedStatement method - {}",
                    batchInsert.size(), e.getMessage()));

            try {
                sqlStatisticValueBatchSaveService.saveBatchStatisticValueSource_PreparedStatement(batchInsert);
                savedCount += batchInsert.size();
            } catch (final SQLException e2) {
                int[] successfulInserts = new int[0];
                int successCount = 0;

                if (e2 instanceof BatchUpdateException) {
                    successfulInserts = ((BatchUpdateException) e2).getUpdateCounts();
                }

                final List<SQLStatisticValueSourceDO> revisedBatch = new ArrayList<>();

                for (int i = 0, lenBatch = batchInsert.size(), lenArr = successfulInserts.length; i < lenBatch; i++) {
                    if (i < lenArr && successfulInserts[i] == 1) {
                        successCount++;
                        // ignore this item as it has already been processed
                    } else {
                        revisedBatch.add(batchInsert.get(i));
                    }
                }

                LOGGER.error(LambdaLogUtil.message(
                        "doSaveBatch() - Failed to insert {} records out of a batch size of {} using PreparedStatement (though succeeded in inserting {}), will try much slower IndividualPreparedStatements method",
                        batchInsert.size() - successCount, batchInsert.size(), successCount), e2);

                final int insertedCount = sqlStatisticValueBatchSaveService
                        .saveBatchStatisticValueSource_IndividualPreparedStatements(revisedBatch);
                savedCount += insertedCount;
                revisedBatch.clear();
            }

        } finally {
            batchInsert.clear();
        }
    }
}
