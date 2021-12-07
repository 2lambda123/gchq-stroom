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

package stroom.search.extraction;

import stroom.data.store.api.DataException;
import stroom.data.store.api.InputStreamProvider;
import stroom.data.store.api.SegmentInputStream;
import stroom.data.store.api.Source;
import stroom.data.store.api.Store;
import stroom.docref.DocRef;
import stroom.meta.shared.Meta;
import stroom.pipeline.PipelineStore;
import stroom.pipeline.errorhandler.ErrorReceiver;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.errorhandler.ProcessException;
import stroom.pipeline.factory.Pipeline;
import stroom.pipeline.factory.PipelineFactory;
import stroom.pipeline.filter.IdEnrichmentFilter;
import stroom.pipeline.filter.XMLFilter;
import stroom.pipeline.shared.data.PipelineData;
import stroom.pipeline.state.CurrentUserHolder;
import stroom.pipeline.state.FeedHolder;
import stroom.pipeline.state.MetaDataHolder;
import stroom.pipeline.state.MetaHolder;
import stroom.pipeline.state.PipelineHolder;
import stroom.pipeline.task.StreamMetaDataProvider;
import stroom.security.api.SecurityContext;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskTerminatedException;
import stroom.util.io.IgnoreCloseInputStream;
import stroom.util.io.StreamUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.logging.SearchProgressLog;
import stroom.util.logging.SearchProgressLog.SearchPhase;
import stroom.util.shared.StoredError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import javax.inject.Inject;

class ExtractionTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractionTaskHandler.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(ExtractionTaskHandler.class);

    private final Store streamStore;
    private final FeedHolder feedHolder;
    private final MetaDataHolder metaDataHolder;
    private final CurrentUserHolder currentUserHolder;
    private final MetaHolder metaHolder;
    private final PipelineHolder pipelineHolder;
    private final ErrorReceiverProxy errorReceiverProxy;
    private final PipelineFactory pipelineFactory;
    private final PipelineStore pipelineStore;
    private final SecurityContext securityContext;

    private ExtractionTask task;

    @Inject
    ExtractionTaskHandler(final Store streamStore,
                          final FeedHolder feedHolder,
                          final MetaDataHolder metaDataHolder,
                          final CurrentUserHolder currentUserHolder,
                          final MetaHolder metaHolder,
                          final PipelineHolder pipelineHolder,
                          final ErrorReceiverProxy errorReceiverProxy,
                          final PipelineFactory pipelineFactory,
                          final PipelineStore pipelineStore,
                          final SecurityContext securityContext) {
        this.streamStore = streamStore;
        this.feedHolder = feedHolder;
        this.metaDataHolder = metaDataHolder;
        this.currentUserHolder = currentUserHolder;
        this.metaHolder = metaHolder;
        this.pipelineHolder = pipelineHolder;
        this.errorReceiverProxy = errorReceiverProxy;
        this.pipelineFactory = pipelineFactory;
        this.pipelineStore = pipelineStore;
        this.securityContext = securityContext;
    }

    public Meta extract(final TaskContext taskContext,
                        final ExtractionTask task,
                        final PipelineData pipelineData) throws DataException {
        Meta meta = null;

        // Open the stream source.
        try (final Source source = streamStore.openSource(task.getStreamId())) {
            if (source != null) {
                SearchProgressLog.increment(SearchPhase.EXTRACTION_TASK_HANDLER_EXTRACT);
                SearchProgressLog.add(SearchPhase.EXTRACTION_TASK_HANDLER_EXTRACT_EVENTS, task.getEventIds().length);

                taskContext.info(() -> "" +
                        "Extracting " +
                        task.getEventIds().length +
                        " records from stream " +
                        task.getStreamId());

                meta = source.getMeta();
                this.task = task;

                // Set the current user.
                currentUserHolder.setCurrentUser(securityContext.getUserId());

                final DocRef pipelineRef = task.getPipelineRef();

                // Create the parser.
                final Pipeline pipeline = pipelineFactory.create(pipelineData, taskContext);
                if (pipeline == null) {
                    throw new ExtractionException("Unable to create parser for pipeline: " + pipelineRef);
                }

                // Set up the id enrichment filter to try and recreate the conditions
                // present when the index was built. We need to do this because the
                // input stream is now filtered to only include events matched by
                // the search. This means that the event ids cannot be calculated by
                // just counting events.
                final String streamId = String.valueOf(task.getStreamId());
                final IdEnrichmentFilter idEnrichmentFilter = getFilter(pipeline, IdEnrichmentFilter.class);
                idEnrichmentFilter.setup(streamId, task.getEventIds());

                // Set up the search result output filter to expect the same order of
                // event ids and give it the result cache and stored data to write
                // values to.
                final AbstractSearchResultOutputFilter searchResultOutputFilter = getFilter(pipeline,
                        AbstractSearchResultOutputFilter.class);

                searchResultOutputFilter.setup(task.getReceiver());

                // Process the stream segments.
                processData(source, task.getEventIds(), pipelineRef, pipeline);

                // Ensure count is the same.
                if (task.getEventIds().length != searchResultOutputFilter.getCount()) {
                    LOGGER.debug("Extraction count mismatch");
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return meta;
    }

    private <T extends XMLFilter> T getFilter(final Pipeline pipeline, final Class<T> clazz) {
        final List<T> filters = pipeline.findFilters(clazz);
        if (filters == null || filters.size() != 1) {
            throw new ExtractionException("Unable to find single '" + clazz.getName() + "' in search result pipeline");
        }
        return filters.get(0);
    }

    /**
     * Extract data from the segment list. Returns the total number of segments
     * that were successfully extracted.
     */
    private void processData(final Source source,
                             final long[] eventIds,
                             final DocRef pipelineRef,
                             final Pipeline pipeline) {
        final ErrorReceiver errorReceiver = (severity, location, elementId, message, e) -> {
            if (!(e instanceof TaskTerminatedException)) {
                final StoredError storedError = new StoredError(severity, location, elementId, message);
                task.getErrorConsumer().add(new RuntimeException(storedError.toString(), e));
            }
            throw ProcessException.wrap(message, e);
        };

        errorReceiverProxy.setErrorReceiver(errorReceiver);
        long count = 0;

        try (final InputStreamProvider inputStreamProvider = source.get(0)) {
            // This is a valid stream so try and extract as many
            // segments as we are allowed.
            try (final SegmentInputStream segmentInputStream = inputStreamProvider.get()) {
                // Include the XML Header and footer.
                segmentInputStream.include(0);
                segmentInputStream.include(segmentInputStream.count() - 1);

                // Include as many segments as we can.
                for (final long eventId : eventIds) {
                    segmentInputStream.include(eventId);
                    count++;
                }

                // Now try and extract the data.
                extract(pipelineRef, pipeline, source, segmentInputStream, count);

            } catch (final RuntimeException e) {
                // Something went wrong extracting data from this
                // stream.
                throw new ExtractionException("Unable to extract data from stream source with id: " +
                        source.getMeta().getId() + " - " + e.getMessage(), e);
            }
        } catch (final ExtractionException e) {
            throw e;
        } catch (final IOException | RuntimeException e) {
            // Something went wrong extracting data from this stream.
            throw new ExtractionException("Unable to extract data from stream source with id: " +
                    source.getMeta().getId() + " - " + e.getMessage(), e);
        }
    }

    /**
     * We do this one by one
     */
    private void extract(final DocRef pipelineRef, final Pipeline pipeline, final Source source,
                         final SegmentInputStream segmentInputStream, final long count) {
        if (source != null && segmentInputStream != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Reading " + count + " segments from stream " + source.getMeta().getId());
            }

            SearchProgressLog.increment(SearchPhase.EXTRACTION_TASK_HANDLER_EXTRACT2);
            SearchProgressLog.add(SearchPhase.EXTRACTION_TASK_HANDLER_EXTRACT2_EVENTS, count);

            try {
                // Here we need to reload the feed as this will get the related
                // objects Translation etc
                feedHolder.setFeedName(source.getMeta().getFeedName());

                // Set up the meta data holder.
                metaDataHolder.setMetaDataProvider(new StreamMetaDataProvider(metaHolder, pipelineStore));

                metaHolder.setMeta(source.getMeta());
                pipelineHolder.setPipeline(pipelineRef);

                final InputStream inputStream = new IgnoreCloseInputStream(segmentInputStream);

                // Get the encoding for the stream we are about to process.
                final String encoding = StreamUtil.DEFAULT_CHARSET_NAME;

                // Process the boundary.
                LAMBDA_LOGGER.logDurationIfDebugEnabled(
                        () -> pipeline.process(inputStream, encoding),
                        () -> LogUtil.message("Processing pipeline {}, stream {}",
                                pipelineRef.getUuid(), source.getMeta().getId()));

            } catch (final TaskTerminatedException e) {
                // Ignore stopped pipeline exceptions as we are meant to get
                // these when a task is asked to stop prematurely.
            } catch (final RuntimeException e) {
                throw ExtractionException.wrap(e);
            }
        }
    }
}
