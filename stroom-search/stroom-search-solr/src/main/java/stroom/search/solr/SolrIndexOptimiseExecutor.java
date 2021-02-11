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

package stroom.search.solr;

import stroom.cluster.lock.api.ClusterLockService;
import stroom.docref.DocRef;
import stroom.search.solr.shared.SolrIndexDoc;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskContextFactory;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogExecutionTime;

import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SolrIndexOptimiseExecutor {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SolrIndexOptimiseExecutor.class);

    private static final String TASK_NAME = "Solr Index Optimise Executor";
    private static final String LOCK_NAME = "SolrIndexOptimiseExecutor";

    private final SolrIndexStore solrIndexStore;
    private final SolrIndexClientCache solrIndexClientCache;
    private final ClusterLockService clusterLockService;
    private final TaskContextFactory taskContextFactory;

    @Inject
    public SolrIndexOptimiseExecutor(final SolrIndexStore solrIndexStore,
                                     final SolrIndexClientCache solrIndexClientCache,
                                     final ClusterLockService clusterLockService,
                                     final TaskContextFactory taskContextFactory) {
        this.solrIndexStore = solrIndexStore;
        this.solrIndexClientCache = solrIndexClientCache;
        this.clusterLockService = clusterLockService;
        this.taskContextFactory = taskContextFactory;
    }

    public void exec() {
        taskContextFactory.context(TASK_NAME, this::exec).run();
    }

    private void exec(final TaskContext taskContext) {
        final LogExecutionTime logExecutionTime = new LogExecutionTime();
        info(taskContext, () -> "Start");
        clusterLockService.tryLock(LOCK_NAME, () -> {
            try {
                if (!Thread.currentThread().isInterrupted()) {
                    final List<DocRef> docRefs = solrIndexStore.list();
                    if (docRefs != null) {
                        docRefs.forEach(docRef -> performRetention(taskContext, docRef));
                    }
                    info(taskContext, () -> "Finished in " + logExecutionTime);
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e::getMessage, e);
            }
        });
    }

    private void performRetention(final TaskContext taskContext, final DocRef docRef) {
        if (!Thread.currentThread().isInterrupted()) {
            try {
                final SolrIndexDoc solrIndexDoc = solrIndexStore.readDocument(docRef);
                if (solrIndexDoc != null) {
                    solrIndexClientCache.context(solrIndexDoc.getSolrConnectionConfig(), solrClient -> {
                        try {
                            info(taskContext, () -> "Optimising '" + solrIndexDoc.getName() + "'");
                            solrClient.optimize(solrIndexDoc.getCollection());
                        } catch (final SolrServerException | IOException e) {
                            LOGGER.error(e::getMessage, e);
                        }
                    });
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e::getMessage, e);
            }
        }
    }

    private void info(final TaskContext taskContext, final Supplier<String> message) {
        taskContext.info(message);
        LOGGER.info(message);
    }
}
