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

package stroom.dashboard.impl.logging;

import stroom.collection.api.CollectionService;
import stroom.dictionary.api.WordListProvider;
import stroom.docref.DocRef;
import stroom.docrefinfo.api.DocRefInfoService;
import stroom.event.logging.api.StroomEventLoggingService;
import stroom.query.api.v2.ExpressionOperator;
import stroom.security.api.SecurityContext;

import event.logging.AdvancedQuery;
import event.logging.Criteria;
import event.logging.DataSources;
import event.logging.Event;
import event.logging.ExportEventAction;
import event.logging.MultiObject;
import event.logging.Purpose;
import event.logging.Query;
import event.logging.SearchEventAction;
import event.logging.util.EventLoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class SearchEventLogImpl implements SearchEventLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEventLogImpl.class);

    private final StroomEventLoggingService eventLoggingService;
    private final SecurityContext securityContext;
    private final WordListProvider wordListProvider;
    private final CollectionService collectionService;
    private final DocRefInfoService docRefInfoService;

    @Inject
    public SearchEventLogImpl(final StroomEventLoggingService eventLoggingService,
                              final SecurityContext securityContext,
                              final WordListProvider wordListProvider,
                              final CollectionService collectionService,
                              final DocRefInfoService docRefInfoService) {
        this.eventLoggingService = eventLoggingService;
        this.securityContext = securityContext;
        this.wordListProvider = wordListProvider;
        this.collectionService = collectionService;
        this.docRefInfoService = docRefInfoService;
    }

    @Override
    public void search(final DocRef dataSourceRef,
                       final ExpressionOperator expression,
                       final String queryInfo) {
        securityContext.insecure(() -> search("Search", dataSourceRef, expression, queryInfo, null));
    }

    @Override
    public void search(final DocRef dataSourceRef,
                       final ExpressionOperator expression,
                       final String queryInfo,
                       final Exception e) {
        securityContext.insecure(() -> search("Search", dataSourceRef, expression, queryInfo, e));
    }

    @Override
    public void batchSearch(final DocRef dataSourceRef,
                            final ExpressionOperator expression,
                            final String queryInfo) {
        securityContext.insecure(() -> search("Batch search", dataSourceRef, expression, queryInfo, null));
    }

    @Override
    public void batchSearch(final DocRef dataSourceRef,
                            final ExpressionOperator expression,
                            final String queryInfo,
                            final Exception e) {
        securityContext.insecure(() -> search("Batch search", dataSourceRef, expression, queryInfo, e));
    }

    @Override
    public void downloadResults(final DocRef dataSourceRef,
                                final ExpressionOperator expression,
                                final String queryInfo) {
        securityContext.insecure(() -> downloadResults("Batch search", dataSourceRef, expression, queryInfo, null));
    }

    @Override
    public void downloadResults(final DocRef dataSourceRef,
                                final ExpressionOperator expression,
                                final String queryInfo,
                                final Exception e) {
        securityContext.insecure(() -> downloadResults("Download search results", dataSourceRef, expression, queryInfo, e));
    }

    @Override
    public void downloadResults(final String type,
                                final DocRef dataSourceRef,
                                final ExpressionOperator expression,
                                final String queryInfo,
                                final Exception e) {
        securityContext.insecure(() -> {
            try {
                final String dataSourceName = getDataSourceName(dataSourceRef);

                final DataSources dataSources = new DataSources();
                dataSources.getDataSource().add(dataSourceName);

                final Criteria criteria = new Criteria();
                criteria.setDataSources(dataSources);
                criteria.setQuery(getQuery(expression));

                final MultiObject multiObject = new MultiObject();
                multiObject.getObjects().add(criteria);

                final ExportEventAction exp = new ExportEventAction();
                exp.setSource(multiObject);
                exp.setOutcome(EventLoggingUtil.createOutcome(e));

                final Event event = eventLoggingService.createSkeletonEvent(type, type + "ing data source \"" + dataSourceRef.toInfoString());

                event.getEventDetail().setEventAction(exp);
                event.getEventDetail().setPurpose(getPurpose(event.getEventDetail().getPurpose(), queryInfo));

                eventLoggingService.log(event);
            } catch (final RuntimeException e2) {
                LOGGER.error(e.getMessage(), e2);
            }
        });
    }

    @Override
    public void search(final String type,
                       final DocRef dataSourceRef,
                       final ExpressionOperator expression,
                       final String queryInfo,
                       final Exception e) {
        securityContext.insecure(() -> {
            try {
                String dataSourceName = getDataSourceName(dataSourceRef);
                if (dataSourceName == null || dataSourceName.isEmpty()) {
                    dataSourceName = "NULL";
                }

                final DataSources dataSources = new DataSources();
                dataSources.getDataSource().add(dataSourceName);

                final SearchEventAction search = new SearchEventAction();
                search.setDataSources(dataSources);
                search.setQuery(getQuery(expression));
                search.setOutcome(EventLoggingUtil.createOutcome(e));

                final Event event = eventLoggingService.createSkeletonEvent(type, type + "ing data source \"" + dataSourceRef.toInfoString());
                event.getEventDetail().setEventAction(search);
                event.getEventDetail().setPurpose(getPurpose(event.getEventDetail().getPurpose(), queryInfo));

                eventLoggingService.log(event);
            } catch (final RuntimeException e2) {
                LOGGER.error(e.getMessage(), e2);
            }
        });
    }

    private String getDataSourceName(final DocRef docRef) {
        if (docRef == null) {
            return null;
        }

        try {
            return docRefInfoService.name(docRef)
                    .orElse(docRef.getName());
        } catch (final RuntimeException e) {
            // We might not have an explorer handler capable of getting info.
            LOGGER.debug(e.getMessage(), e);
        }

        return docRef.getName();
    }

    private Purpose getPurpose(Purpose purpose, final String queryInfo) {
        if (null != queryInfo) {
            if (purpose == null) {
                purpose = new Purpose();
            }

            purpose.setJustification(queryInfo);
        }

        return purpose;
    }

    private Query getQuery(final ExpressionOperator expression) {
        final Query query = new Query();
        final AdvancedQuery advanced = new AdvancedQuery();
        query.setAdvanced(advanced);
        QueryDataLogUtil.appendExpressionItem(
                advanced.getQueryItems(),
                wordListProvider,
                collectionService,
                expression);
        return query;
    }
}
