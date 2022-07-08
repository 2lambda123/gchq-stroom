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

import stroom.cluster.api.ClusterRoles;
import stroom.cluster.api.ClusterService;
import stroom.docref.DocRef;
import stroom.search.solr.shared.SolrIndexDoc;
import stroom.task.api.TaskContext;
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

    private static final String LOCK_NAME = "SolrIndexOptimiseExecutor";

    private final SolrIndexStore solrIndexStore;
    private final SolrIndexClientCache solrIndexClientCache;
    private final ClusterService clusterService;
    private final TaskContext taskContext;

    @Inject
    public SolrIndexOptimiseExecutor(final SolrIndexStore solrIndexStore,
                                     final SolrIndexClientCache solrIndexClientCache,
                                     final ClusterService clusterService,
                                     final TaskContext taskContext) {
        this.solrIndexStore = solrIndexStore;
        this.solrIndexClientCache = solrIndexClientCache;
        this.clusterService = clusterService;
        this.taskContext = taskContext;
    }

    public void exec() {
        final LogExecutionTime logExecutionTime = new LogExecutionTime();
        info(() -> "Start");
        if (clusterService.isLeaderForRole(ClusterRoles.SOLR_INDEX_OPTIMISE)) {
            clusterService.tryLock(LOCK_NAME, () -> {
                try {
                    if (!Thread.currentThread().isInterrupted()) {
                        final List<DocRef> docRefs = solrIndexStore.list();
                        if (docRefs != null) {
                            docRefs.forEach(this::performRetention);
                        }
                        info(() -> "Finished in " + logExecutionTime);
                    }
                } catch (final RuntimeException e) {
                    LOGGER.error(e::getMessage, e);
                }
            });
        }
    }

    private void performRetention(final DocRef docRef) {
        if (!Thread.currentThread().isInterrupted()) {
            try {
                final SolrIndexDoc solrIndexDoc = solrIndexStore.readDocument(docRef);
                if (solrIndexDoc != null) {
                    solrIndexClientCache.context(solrIndexDoc.getSolrConnectionConfig(), solrClient -> {
                        try {
                            info(() -> "Optimising '" + solrIndexDoc.getName() + "'");
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

    private void info(final Supplier<String> message) {
        taskContext.info(message);
        LOGGER.info(message);
    }
}
