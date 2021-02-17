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

package stroom.core.receive;

import stroom.proxy.repo.FileSetProcessor;
import stroom.proxy.repo.NewRepositoryProcessor;
import stroom.proxy.repo.ZipInfoStore;
import stroom.task.api.ExecutorProvider;
import stroom.task.api.TaskContextFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * <p>
 * Task read a proxy repository and sent it to the stream store.
 * </p>
 */
public class NewProxyAggregationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewProxyAggregationExecutor.class);

    private final NewRepositoryProcessor repositoryProcessor;

    @Inject
    NewProxyAggregationExecutor(final ExecutorProvider executorProvider,
                                final TaskContextFactory taskContextFactory,
                                final Provider<FileSetProcessor> fileSetProcessorProvider,
                                final ProxyAggregationConfig proxyAggregationConfig,
                                final ZipInfoStore zipInfoRetriever) {
        this(
                executorProvider,
                taskContextFactory,
                fileSetProcessorProvider,
                proxyAggregationConfig.getProxyDir(),
                proxyAggregationConfig.getProxyThreads(),
                proxyAggregationConfig.getMaxFileScan(),
                proxyAggregationConfig.getMaxConcurrentMappedFiles(),
                proxyAggregationConfig.getMaxFilesPerAggregate(),
                proxyAggregationConfig.getMaxUncompressedFileSizeBytes(),
                zipInfoRetriever
        );
    }

    public NewProxyAggregationExecutor(final ExecutorProvider executorProvider,
                                       final TaskContextFactory taskContextFactory,
                                       final Provider<FileSetProcessor> fileSetProcessorProvider,
                                       final String proxyDir,
                                       final int threadCount,
                                       final int maxFileScan,
                                       final int maxConcurrentMappedFiles,
                                       final int maxFilesPerAggregate,
                                       final long maxUncompressedFileSize,
                                       final ZipInfoStore zipInfoRetriever) {
        this.repositoryProcessor = new NewRepositoryProcessor(
                executorProvider,
                taskContextFactory,
                fileSetProcessorProvider,
                proxyDir,
                threadCount,
                maxFileScan,
                maxConcurrentMappedFiles,
                maxFilesPerAggregate,
                maxUncompressedFileSize,
                zipInfoRetriever);
    }

    public void exec() {
        if (!Thread.currentThread().isInterrupted()) {
            try {
                repositoryProcessor.process();
            } catch (final Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
