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

package stroom.search.solr.search;

import stroom.annotation.api.AnnotationFields;
import stroom.pipeline.errorhandler.MessageUtil;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.common.v2.CompletionState;
import stroom.search.coprocessor.Coprocessors;
import stroom.search.coprocessor.CoprocessorsFactory;
import stroom.search.coprocessor.Error;
import stroom.search.coprocessor.NewCoprocessor;
import stroom.search.coprocessor.Receiver;
import stroom.search.coprocessor.ReceiverImpl;
import stroom.search.extraction.ExpressionFilter;
import stroom.search.extraction.ExtractionDecoratorFactory;
import stroom.search.resultsender.NodeResult;
import stroom.search.resultsender.ResultSender;
import stroom.search.resultsender.ResultSenderFactory;
import stroom.security.api.SecurityContext;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskTerminatedException;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

class SolrClusterSearchTaskHandler implements Consumer<Error> {
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SolrClusterSearchTaskHandler.class);

    private final CoprocessorsFactory coprocessorsFactory;
    private final SolrSearchFactory solrSearchFactory;
    private final ExtractionDecoratorFactory extractionDecoratorFactory;
    private final ResultSenderFactory resultSenderFactory;
    private final SecurityContext securityContext;
    private final LinkedBlockingQueue<String> errors = new LinkedBlockingQueue<>();
    private final CompletionState searchCompletionState = new CompletionState();

    private SolrClusterSearchTask task;

    @Inject
    SolrClusterSearchTaskHandler(final CoprocessorsFactory coprocessorsFactory,
                                 final SolrSearchFactory solrSearchFactory,
                                 final ExtractionDecoratorFactory extractionDecoratorFactory,
                                 final ResultSenderFactory resultSenderFactory,
                                 final SecurityContext securityContext) {
        this.coprocessorsFactory = coprocessorsFactory;
        this.solrSearchFactory = solrSearchFactory;
        this.extractionDecoratorFactory = extractionDecoratorFactory;
        this.resultSenderFactory = resultSenderFactory;
        this.securityContext = securityContext;
    }

    public void exec(final TaskContext taskContext, final SolrClusterSearchTask task, final SolrSearchResultCollector callback) {
        securityContext.useAsRead(() -> {
            final Consumer<NodeResult> resultConsumer = callback::onSuccess;
            CompletionState sendingDataCompletionState = new CompletionState();
            sendingDataCompletionState.complete();

            if (!Thread.currentThread().isInterrupted()) {
                taskContext.info(() -> "Initialising...");

                this.task = task;
                final stroom.query.api.v2.Query query = task.getQuery();

                try {
                    final long frequency = task.getResultSendFrequency();

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
                    final Coprocessors coprocessors = coprocessorsFactory.create(task.getSettings(), storedFields, query.getParams(), this);

                    if (coprocessors.size() > 0) {
                        // Start forwarding data to target node.
                        final ResultSender resultSender = resultSenderFactory.create(taskContext);
                        sendingDataCompletionState = resultSender.sendData(coprocessors, resultConsumer, frequency, searchCompletionState, errors);

                        // Start searching.
                        search(taskContext, task, query, coprocessors);
                    }
                } catch (final RuntimeException e) {
                    try {
                        callback.onFailure(e);
                    } catch (final RuntimeException e2) {
                        // If we failed to send the result or the source node rejected the result because the source task has been terminated then terminate the task.
                        LOGGER.info(() -> "Terminating search because we were unable to send result");
                        Thread.currentThread().interrupt();
                    }
                } finally {
                    LOGGER.trace(() -> "Search is complete, setting searchComplete to true and " +
                            "counting down searchCompleteLatch");
                    // Tell the client that the search has completed.
                    searchCompletionState.complete();
                }

                // Now we must wait for results to be sent to the requesting node.
                try {
                    taskContext.info(() -> "Sending final results");
                    while (!Thread.currentThread().isInterrupted() && !sendingDataCompletionState.isComplete()) {
                        sendingDataCompletionState.awaitCompletion(1, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    //Don't want to reset interrupt status as this thread will go back into
                    //the executor's pool. Throwing an exception will terminate the task
                    throw new RuntimeException("Thread interrupted");
                }
            }
        });
    }

    private void search(final TaskContext taskContext,
                        final SolrClusterSearchTask task,
                        final stroom.query.api.v2.Query query,
                        final Coprocessors coprocessors) {
        taskContext.info(() -> "Searching...");
        LOGGER.debug(() -> "Incoming search request:\n" + query.getExpression().toString());

        try {
            final AtomicLong allDocumentCount = new AtomicLong();
            final Receiver rootReceiver = new ReceiverImpl(null, this, allDocumentCount::addAndGet, null);
            final Receiver extractionReceiver = extractionDecoratorFactory.create(taskContext, rootReceiver, task.getStoredFields(), coprocessors, query);

            // Search all index shards.
            final ExpressionFilter expressionFilter = new ExpressionFilter.Builder()
                    .addPrefixExcludeFilter(AnnotationFields.ANNOTATION_FIELD_PREFIX)
                    .build();
            final ExpressionOperator expression = expressionFilter.copy(task.getQuery().getExpression());
            solrSearchFactory.search(task, expression, extractionReceiver, taskContext);

            // Wait for index search completion.
            long extractionCount = getMinExtractions(coprocessors.getSet());
            long documentCount = allDocumentCount.get();
            while (!Thread.currentThread().isInterrupted() && extractionCount < documentCount) {
                log(taskContext, extractionCount, documentCount);

                Thread.sleep(1000);

                extractionCount = getMinExtractions(coprocessors.getSet());
                documentCount = allDocumentCount.get();
            }

            LOGGER.debug(() -> "Complete");

        } catch (final InterruptedException e) {
            // Continue to interrupt.
            Thread.currentThread().interrupt();
            LOGGER.debug(e::getMessage, e);
        } catch (final RuntimeException e) {
            throw SearchException.wrap(e);
        }
    }

    private void log(final TaskContext taskContext, final long extractionCount, final long documentCount) {
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

    public SolrClusterSearchTask getTask() {
        return task;
    }
}
