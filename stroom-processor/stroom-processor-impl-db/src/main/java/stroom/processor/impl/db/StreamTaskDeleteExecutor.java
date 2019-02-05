/*
 * Copyright 2017 Crown Copyright
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

package stroom.processor.impl.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.cluster.lock.api.ClusterLockService;
import stroom.entity.StroomEntityManager;
import stroom.entity.shared.Period;
import stroom.entity.util.SqlBuilder;
import stroom.processor.ProcessorConfig;
import stroom.processor.StreamProcessorFilterService;
import stroom.streamtask.shared.FindStreamProcessorFilterCriteria;
import stroom.processor.shared.ProcessorFilter;
import stroom.processor.shared.ProcessorFilterTask;
import stroom.streamtask.shared.ProcessorFilterTracker;
import stroom.streamtask.shared.TaskStatus;
import stroom.task.api.TaskContext;
import stroom.util.date.DateUtil;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

class StreamTaskDeleteExecutor extends AbstractBatchDeleteExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamTaskDeleteExecutor.class);

    private static final String TASK_NAME = "Stream Task Delete Executor";
    private static final String LOCK_NAME = "StreamTaskDeleteExecutor";
    private static final String TEMP_STRM_TASK_ID_TABLE = "TEMP_STRM_TASK_ID";

    private final StreamTaskCreatorImpl streamTaskCreator;
    private final StreamProcessorFilterService streamProcessorFilterService;
    private final StroomEntityManager stroomEntityManager;

    @Inject
    StreamTaskDeleteExecutor(final BatchIdTransactionHelper batchIdTransactionHelper,
                             final ClusterLockService clusterLockService,
                             final ProcessorConfig processorConfig,
                             final TaskContext taskContext,
                             final StreamTaskCreatorImpl streamTaskCreator,
                             final StreamProcessorFilterService streamProcessorFilterService,
                             final StroomEntityManager stroomEntityManager) {
        super(batchIdTransactionHelper, clusterLockService, taskContext, TASK_NAME, LOCK_NAME,
                processorConfig, TEMP_STRM_TASK_ID_TABLE);
        this.streamTaskCreator = streamTaskCreator;
        this.streamProcessorFilterService = streamProcessorFilterService;
        this.stroomEntityManager = stroomEntityManager;
    }

    public void exec() {
        final AtomicLong nextDeleteMs = streamTaskCreator.getNextDeleteMs();

        try {
            if (nextDeleteMs.get() == 0) {
                LOGGER.debug("deleteSchedule() - no schedule set .... maybe we aren't in charge of creating tasks");
            } else {
                LOGGER.debug("deleteSchedule() - nextDeleteMs={}",
                        DateUtil.createNormalDateTimeString(nextDeleteMs.get()));
                // Have we gone past our next delete schedule?
                if (nextDeleteMs.get() < System.currentTimeMillis()) {
                    lockAndDelete();
                }
            }
        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void delete(final long age) {
        super.delete(age);
        deleteOldFilters(age);
    }

    @Override
    protected void deleteCurrentBatch(final long total) {
        // Delete stream tasks.
        deleteWithJoin(ProcessorFilterTask.TABLE_NAME, ProcessorFilterTask.ID, "stream tasks", total);
    }

    @Override
    protected List<Long> getDeleteIdList(final long age, final int batchSize) {
        final SqlBuilder sql = new SqlBuilder();
        sql.append("SELECT ");
        sql.append(ProcessorFilterTask.ID);
        sql.append(" FROM ");
        sql.append(ProcessorFilterTask.TABLE_NAME);
        sql.append(" WHERE ");
        sql.append(ProcessorFilterTask.STATUS);
        sql.append(" IN (");
        sql.append(TaskStatus.COMPLETE.getPrimitiveValue());
        sql.append(", ");
        sql.append(TaskStatus.FAILED.getPrimitiveValue());
        sql.append(") AND (");
        sql.append(ProcessorFilterTask.CREATE_MS);
        sql.append(" IS NULL OR ");
        sql.append(ProcessorFilterTask.CREATE_MS);
        sql.append(" < ");
        sql.arg(age);
        sql.append(")");
        sql.append(" ORDER BY ");
        sql.append(ProcessorFilterTask.ID);
        sql.append(" LIMIT ");
        sql.arg(batchSize);
        return stroomEntityManager.executeNativeQueryResultList(sql);
    }

    private void deleteOldFilters(final long age) {
        try {
            // Get all filters that have not been polled for a while.
            final FindStreamProcessorFilterCriteria criteria = new FindStreamProcessorFilterCriteria();
            criteria.setLastPollPeriod(new Period(null, age));
            final List<ProcessorFilter> filters = streamProcessorFilterService.find(criteria);
            for (final ProcessorFilter filter : filters) {
                final ProcessorFilterTracker tracker = filter.getStreamProcessorFilterTracker();

                if (tracker != null && ProcessorFilterTracker.COMPLETE.equals(tracker.getStatus())) {
                    // The tracker thinks that no more tasks will ever be
                    // created for this filter so we can delete it if there are
                    // no remaining tasks for this filter.
                    //
                    // The database constraint will not allow filters to be
                    // deleted that still have associated tasks.
                    try {
                        LOGGER.debug("deleteCompleteOrFailedTasks() - Removing old complete filter {}", filter);
                        streamProcessorFilterService.delete(filter);

                    } catch (final RuntimeException e) {
                        // The database constraint will not allow filters to be
                        // deleted that still have associated tasks. This is
                        // what we want to happen but output debug here to help
                        // diagnose problems.
                        LOGGER.debug("deleteCompleteOrFailedTasks() - Failed as tasks still remain for this filter - "
                                + e.getMessage(), e);
                    }
                }
            }
        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
