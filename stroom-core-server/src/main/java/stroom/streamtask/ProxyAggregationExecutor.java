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

package stroom.streamtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.proxy.repo.RepositoryProcessor;
import stroom.proxy.repo.StroomZipRepository;
import stroom.task.ExecutorProvider;
import stroom.task.ThreadPoolImpl;
import stroom.task.api.TaskContext;
import stroom.task.shared.Task;
import stroom.task.shared.ThreadPool;
import stroom.util.date.DateUtil;
import stroom.util.lifecycle.JobTrackedSchedule;
import stroom.util.lifecycle.StroomSimpleCronSchedule;
import stroom.util.logging.LogExecutionTime;
import stroom.util.shared.ModelStringUtil;

import javax.inject.Inject;

/**
 * <p>
 * Task read a proxy repository and sent it to the stream store.
 * </p>
 */
class ProxyAggregationExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyAggregationExecutor.class);

    private final TaskContext taskContext;
    private final String proxyDir;
    private final int maxAggregation;
    private final ProxyFileProcessorImpl proxyFileProcessor;
    private final RepositoryProcessor repositoryProcessor;
    private final long maxStreamSize;

    @Inject
    ProxyAggregationExecutor(final ProxyFileProcessorImpl proxyFileProcessor,
                             final TaskContext taskContext,
                             final ExecutorProvider executorProvider,
                             final ProxyAggregationConfig proxyAggregationConfig) {
        this(
                proxyFileProcessor,
                taskContext,
                executorProvider,
                proxyAggregationConfig.getProxyDir(),
                proxyAggregationConfig.getProxyThreads(),
                proxyAggregationConfig.getMaxAggregation(),
                proxyAggregationConfig.getMaxAggregationScan(),
                proxyAggregationConfig.getMaxStreamSizeBytes()
        );
    }

    ProxyAggregationExecutor(final ProxyFileProcessorImpl proxyFileProcessor,
                             final TaskContext taskContext,
                             final ExecutorProvider executorProvider,
                             final String proxyDir,
                             final int threadCount,
                             final int maxAggregation,
                             final int maxFileScan,
                             final long maxStreamSize) {
        this.proxyFileProcessor = proxyFileProcessor;
        this.taskContext = taskContext;
        this.proxyDir = proxyDir;
        this.maxAggregation = maxAggregation;
        this.maxStreamSize = maxStreamSize;

        final ThreadPool threadPool = new ThreadPoolImpl("Proxy Aggregation", 5, 0, threadCount);

        repositoryProcessor = new RepositoryProcessor(proxyFileProcessor, executorProvider.getExecutor(threadPool), taskContext);
        repositoryProcessor.setMaxFileScan(maxFileScan);
    }

    @StroomSimpleCronSchedule(cron = "0,10,20,30,40,50 * *")
    @JobTrackedSchedule(jobName = "Proxy Aggregation", advanced = false, description = "Job to pick up the data written by the proxy and store it in Stroom")
    public void exec() {
        try {
            final LogExecutionTime logExecutionTime = new LogExecutionTime();
            LOGGER.info("exec() - started");

            boolean complete = false;
            while (!complete && !Thread.interrupted()) {
                taskContext.info("Aggregate started {}, maxAggregation {}, maxAggregationScan {}, maxStreamSize {}",
                        DateUtil.createNormalDateTimeString(System.currentTimeMillis()),
                        ModelStringUtil.formatCsv(maxAggregation),
                        ModelStringUtil.formatCsv(repositoryProcessor.getMaxFileScan()),
                        ModelStringUtil.formatIECByteSizeString(maxStreamSize));

                final StroomZipRepository stroomZipRepository = new StroomZipRepository(this.proxyDir);
                complete = repositoryProcessor.process(stroomZipRepository);
            }

            LOGGER.info("exec() - completed in {}", logExecutionTime);
        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


//    private void startExecutor() {
//        taskPool = new AsyncTaskHelper<>(null, taskContext, taskManager, threadCount);
//    }
//
//    private void stopExecutor(final boolean now) {
//        if (taskPool != null) {
//            if (now) {
//                taskContext.terminate();
//                taskPool.clear();
//            }
//            taskPool.join();
//        }
//    }
//
//    private void waitForComplete() {
//        taskPool.join();
//    }
//
//    private void execute(final String message, final Runnable runnable) {
//        if (!stop) {
//            final GenericServerTask genericServerTask = GenericServerTask.create(task, task.getTaskName(), message);
//            genericServerTask.setRunnable(runnable);
//            taskPool.fork(genericServerTask);
//        }
//    }


    /**
     * Stops the task as soon as possible.
     */
    private void stop() {
        LOGGER.info("stop() - Proxy Aggregation - Stopping");
        proxyFileProcessor.stop();
    }
}
