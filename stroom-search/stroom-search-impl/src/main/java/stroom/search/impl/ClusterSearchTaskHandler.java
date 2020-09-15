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

package stroom.search.impl;

import stroom.annotation.api.AnnotationFields;
import stroom.pipeline.errorhandler.MessageUtil;
import stroom.query.api.v2.ExpressionOperator;
import stroom.search.coprocessor.Coprocessors;
import stroom.search.coprocessor.CoprocessorsFactory;
import stroom.search.coprocessor.Error;
import stroom.search.coprocessor.NewCoprocessor;
import stroom.search.coprocessor.Receiver;
import stroom.search.coprocessor.ReceiverImpl;
import stroom.search.extraction.ExpressionFilter;
import stroom.search.extraction.ExtractionDecoratorFactory;
import stroom.search.impl.shard.IndexShardSearchFactory;
import stroom.security.api.SecurityContext;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskTerminatedException;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

class ClusterSearchTaskHandler implements Consumer<Error> {
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ClusterSearchTaskHandler.class);

    private final CoprocessorsFactory coprocessorsFactory;
    private final IndexShardSearchFactory indexShardSearchFactory;
    private final ExtractionDecoratorFactory extractionDecoratorFactory;
    private final RemoteSearchResults remoteSearchResults;
    private final SecurityContext securityContext;
    private final LinkedBlockingQueue<String> errors = new LinkedBlockingQueue<>();

    private ClusterSearchTask task;

    @Inject
    ClusterSearchTaskHandler(final CoprocessorsFactory coprocessorsFactory,
                             final IndexShardSearchFactory indexShardSearchFactory,
                             final ExtractionDecoratorFactory extractionDecoratorFactory,
                             final RemoteSearchResults remoteSearchResults,
                             final SecurityContext securityContext) {
        this.coprocessorsFactory = coprocessorsFactory;
        this.indexShardSearchFactory = indexShardSearchFactory;
        this.extractionDecoratorFactory = extractionDecoratorFactory;
        this.remoteSearchResults = remoteSearchResults;
        this.securityContext = securityContext;
    }

    public void exec(final TaskContext taskContext, final ClusterSearchTask task) {
        final Optional<RemoteSearchResultFactory> optional = remoteSearchResults.get(task.getKey());
        final RemoteSearchResultFactory resultFactory = optional.orElseThrow(() ->
                new SearchException("No search result factory can be found"));

        securityContext.useAsRead(() -> {
            if (!Thread.currentThread().isInterrupted()) {
                taskContext.info(() -> "Initialising...");

                this.task = task;
                final stroom.query.api.v2.Query query = task.getQuery();

                try {
                    // Make sure we have been given a query.
                    if (query.getExpression() == null) {
                        throw new SearchException("Search expression has not been set");
                    }

                    // Get the stored fields that search is hoping to use.
                    final String[] storedFields = task.getStoredFields();
                    if (storedFields == null || storedFields.length == 0) {
                        throw new SearchException("No stored fields have been requested");
                    }

                    // Create coprocessors.
                    final Coprocessors coprocessors = coprocessorsFactory.create(
                            task.getCoprocessorMap(),
                            storedFields,
                            query.getParams(),
                            this);

                    // Start forwarding data to target node.
                    resultFactory.setCoprocessors(coprocessors);
                    resultFactory.setErrors(errors);
                    resultFactory.setTaskContext(taskContext);
                    resultFactory.setStarted(true);

                    if (coprocessors.size() > 0 && !Thread.currentThread().isInterrupted()) {
                        // Start searching.
                        search(taskContext, task, query, coprocessors);
                    }
                } catch (final RuntimeException e) {
                    errors.add(e.getMessage());
                } finally {
                    LOGGER.trace(() -> "Search is complete, setting searchComplete to true and " +
                            "counting down searchCompleteLatch");
                    // Tell the client that the search has completed.
                    resultFactory.getCompletionState().complete();
                }
            }
        });
    }

    private void search(final TaskContext taskContext,
                        final ClusterSearchTask task,
                        final stroom.query.api.v2.Query query,
                        final Coprocessors coprocessors) {
        taskContext.info(() -> "Searching...");
        LOGGER.debug(() -> "Incoming search request:\n" + query.getExpression().toString());

        try {
            if (task.getShards().size() > 0) {
                final AtomicLong allDocumentCount = new AtomicLong();
                final Receiver rootReceiver = new ReceiverImpl(null, this, allDocumentCount::addAndGet, null);
                final Receiver extractionReceiver = extractionDecoratorFactory.create(taskContext, rootReceiver, task.getStoredFields(), coprocessors, query);

                // Search all index shards.
                final ExpressionFilter expressionFilter = new ExpressionFilter.Builder()
                        .addPrefixExcludeFilter(AnnotationFields.ANNOTATION_FIELD_PREFIX)
                        .build();
                final ExpressionOperator expression = expressionFilter.copy(task.getQuery().getExpression());
                indexShardSearchFactory.search(taskContext, task, expression, extractionReceiver);

                // Wait for index search completion.
                long extractionCount = getMinExtractions(coprocessors.getSet());
                long documentCount = allDocumentCount.get();
                while (!Thread.currentThread().isInterrupted() && extractionCount < documentCount) {
                    log(taskContext, documentCount, extractionCount);

                    Thread.sleep(1000);

                    extractionCount = getMinExtractions(coprocessors.getSet());
                    documentCount = allDocumentCount.get();
                }

                LOGGER.debug(() -> "Complete");
                Thread.currentThread().interrupt();
            }
        } catch (final RuntimeException pEx) {
            throw SearchException.wrap(pEx);
        } catch (final InterruptedException pEx) {
            // Continue to interrupt.
            Thread.currentThread().interrupt();
            throw SearchException.wrap(pEx);
        }
    }

    private void log(final TaskContext taskContext, final long documentCount, final long extractionCount) {
        taskContext.info(() ->
                "Searching... " +
                        "found " + documentCount + " documents" +
                        " performed " + extractionCount + " extractions");
    }

    private long getMinExtractions(final Set<NewCoprocessor> coprocessorConsumers) {
        return coprocessorConsumers.stream().mapToLong(NewCoprocessor::getCompletionCount).min().orElse(0);
    }

    @Override
    public void accept(final Error error) {
        if (error != null) {
            LOGGER.debug(error::getMessage, error.getThrowable());
            if (!(error.getThrowable() instanceof TaskTerminatedException)) {
                final String msg = MessageUtil.getMessage(error.getMessage(), error.getThrowable());
                errors.offer(msg);
            }
        }
    }

    public ClusterSearchTask getTask() {
        return task;
    }
}
