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

import event.logging.Criteria;
import event.logging.Criteria.DataSources;
import event.logging.Event;
import event.logging.Export;
import event.logging.MultiObject;
import event.logging.Purpose;
import event.logging.Query;
import event.logging.Query.Advanced;
import event.logging.Search;
import event.logging.util.EventLoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.dictionary.api.DictionaryStore;
import stroom.docref.DocRef;
import stroom.event.logging.api.StroomEventLoggingService;
import stroom.query.api.v2.ExpressionOperator;
import stroom.security.api.Security;

import javax.inject.Inject;

public class SearchEventLogImpl implements SearchEventLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEventLogImpl.class);

    private final StroomEventLoggingService eventLoggingService;
    private final Security security;
    private final DictionaryStore dictionaryStore;

    @Inject
    public SearchEventLogImpl(final StroomEventLoggingService eventLoggingService,
                              final Security security,
                              final DictionaryStore dictionaryStore) {
        this.eventLoggingService = eventLoggingService;
        this.security = security;
        this.dictionaryStore = dictionaryStore;
    }

    @Override
    public void search(final DocRef dataSourceRef,
                       final ExpressionOperator expression,
                       final String queryInfo) {
        security.insecure(() -> search("Search", dataSourceRef, expression, queryInfo, null));
    }

    @Override
    public void search(final DocRef dataSourceRef,
                       final ExpressionOperator expression,
                       final String queryInfo,
                       final Exception e) {
        security.insecure(() -> search("Search", dataSourceRef, expression, queryInfo, e));
    }

    @Override
    public void batchSearch(final DocRef dataSourceRef,
                            final ExpressionOperator expression,
                            final String queryInfo) {
        security.insecure(() -> search("Batch search", dataSourceRef, expression, queryInfo, null));
    }

    @Override
    public void batchSearch(final DocRef dataSourceRef,
                            final ExpressionOperator expression,
                            final String queryInfo,
                            final Exception e) {
        security.insecure(() -> search("Batch search", dataSourceRef, expression, queryInfo, e));
    }

    @Override
    public void downloadResults(final DocRef dataSourceRef,
                                final ExpressionOperator expression,
                                final String queryInfo) {
        security.insecure(() -> downloadResults("Batch search", dataSourceRef, expression, queryInfo, null));
    }

    @Override
    public void downloadResults(final DocRef dataSourceRef,
                                final ExpressionOperator expression,
                                final String queryInfo,
                                final Exception e) {
        security.insecure(() -> downloadResults("Download search results", dataSourceRef, expression, queryInfo, e));
    }

    @Override
    public void downloadResults(final String type,
                                final DocRef dataSourceRef,
                                final ExpressionOperator expression,
                                final String queryInfo,
                                final Exception e) {
        security.insecure(() -> {
            try {
                final String dataSourceName = getDataSourceName(dataSourceRef);

                final DataSources dataSources = new DataSources();
                dataSources.getDataSource().add(dataSourceName);

                final Criteria criteria = new Criteria();
                criteria.setDataSources(dataSources);
                criteria.setQuery(getQuery(expression));

                final MultiObject multiObject = new MultiObject();
                multiObject.getObjects().add(criteria);

                final Export exp = new Export();
                exp.setSource(multiObject);
                exp.setOutcome(EventLoggingUtil.createOutcome(e));

                final Event event = eventLoggingService.createAction(type, type + "ing data source \"" + dataSourceRef.toInfoString());

                event.getEventDetail().setExport(exp);
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
        security.insecure(() -> {
            try {
                String dataSourceName = getDataSourceName(dataSourceRef);
                if (dataSourceName == null || dataSourceName.isEmpty()) {
                    dataSourceName = "NULL";
                }

                final DataSources dataSources = new DataSources();
                dataSources.getDataSource().add(dataSourceName);

                final Search search = new Search();
                search.setDataSources(dataSources);
                search.setQuery(getQuery(expression));
                search.setOutcome(EventLoggingUtil.createOutcome(e));

                final Event event = eventLoggingService.createAction(type, type + "ing data source \"" + dataSourceRef.toInfoString());
                event.getEventDetail().setSearch(search);
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

//        final DataSource dataSource = dataSourceProviderRegistry.getDataSource(docRef);
//        if (dataSource == null) {
//            return null;
//        }

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
        final Advanced advanced = new Advanced();
        query.setAdvanced(advanced);
        QueryDataLogUtil.appendExpressionItem(advanced.getAdvancedQueryItems(), dictionaryStore, expression);
        return query;
    }
}
