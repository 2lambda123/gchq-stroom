/*
 * Copyright 2024 Crown Copyright
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

package stroom.search.solr.search;

import stroom.datasource.api.v2.FindFieldCriteria;
import stroom.datasource.api.v2.IndexField;
import stroom.datasource.api.v2.QueryField;
import stroom.dictionary.api.WordListProvider;
import stroom.docref.DocRef;
import stroom.expression.api.DateTimeSettings;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionUtil;
import stroom.query.api.v2.Query;
import stroom.query.api.v2.SearchRequest;
import stroom.query.common.v2.CoprocessorSettings;
import stroom.query.common.v2.CoprocessorsFactory;
import stroom.query.common.v2.CoprocessorsImpl;
import stroom.query.common.v2.DataStoreSettings;
import stroom.query.common.v2.FieldInfoResultPageBuilder;
import stroom.query.common.v2.IndexFieldCache;
import stroom.query.common.v2.IndexFieldMap;
import stroom.query.common.v2.IndexFieldProvider;
import stroom.query.common.v2.ResultStore;
import stroom.query.common.v2.ResultStoreFactory;
import stroom.query.common.v2.SearchProvider;
import stroom.search.solr.SolrIndexStore;
import stroom.search.solr.search.SearchExpressionQueryBuilder.SearchExpressionQuery;
import stroom.search.solr.shared.SolrIndexDataSourceFieldUtil;
import stroom.search.solr.shared.SolrIndexDoc;
import stroom.security.api.SecurityContext;
import stroom.util.shared.GwtNullSafe;
import stroom.util.shared.ResultPage;
import stroom.util.shared.string.CIKey;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// used by DI
@SuppressWarnings("unused")
public class SolrSearchProvider implements SearchProvider, IndexFieldProvider {

    public static final String ENTITY_TYPE = SolrIndexDoc.DOCUMENT_TYPE;
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrSearchProvider.class);
    private static final int SEND_INTERACTIVE_SEARCH_RESULT_FREQUENCY = 500;
    private static final int DEFAULT_MAX_BOOLEAN_CLAUSE_COUNT = 1024;

    private final WordListProvider wordListProvider;
    private final SolrSearchConfig searchConfig;
    private final SecurityContext securityContext;
    private final CoprocessorsFactory coprocessorsFactory;
    private final ResultStoreFactory resultStoreFactory;
    private final SolrIndexStore solrIndexStore;
    private final SolrSearchExecutor solrSearchExecutor;
    private final IndexFieldCache indexFieldCache;

    @Inject
    public SolrSearchProvider(final WordListProvider wordListProvider,
                              final SolrSearchConfig searchConfig,
                              final CoprocessorsFactory coprocessorsFactory,
                              final ResultStoreFactory resultStoreFactory,
                              final SolrIndexStore solrIndexStore,
                              final SecurityContext securityContext,
                              final SolrSearchExecutor solrSearchExecutor,
                              final IndexFieldCache indexFieldCache) {
        this.wordListProvider = wordListProvider;
        this.searchConfig = searchConfig;
        this.coprocessorsFactory = coprocessorsFactory;
        this.resultStoreFactory = resultStoreFactory;
        this.solrIndexStore = solrIndexStore;
        this.securityContext = securityContext;
        this.solrSearchExecutor = solrSearchExecutor;
        this.indexFieldCache = indexFieldCache;
    }

    @Override
    public ResultPage<QueryField> getFieldInfo(final FindFieldCriteria criteria) {
        return securityContext.useAsReadResult(() -> {
            final FieldInfoResultPageBuilder builder = FieldInfoResultPageBuilder.builder(criteria);
            final SolrIndexDoc index = solrIndexStore.readDocument(criteria.getDataSourceRef());
            if (index != null) {
                final List<QueryField> fields = SolrIndexDataSourceFieldUtil.getDataSourceFields(index);
                builder.addAll(fields);
            }
            return builder.build();
        });
    }

    @Override
    public IndexFieldMap getIndexFields(final DocRef docRef, final CIKey fieldName) {
        final SolrIndexDoc index = solrIndexStore.readDocument(docRef);
        if (index != null && index.getFields() != null) {
            final Map<String, IndexField> fieldMap = index
                    .getFields()
                    .stream()
                    .filter(field ->
                            CIKey.equalsIgnoreCase(fieldName, field.getFldName()))
                    .map(solrIndexField -> (IndexField) solrIndexField)
                    .collect(Collectors.toMap(IndexField::getFldName, Function.identity()));

            if (GwtNullSafe.hasEntries(fieldMap)) {
                return IndexFieldMap.fromFieldsMap(fieldName, fieldMap);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public Optional<String> fetchDocumentation(final DocRef docRef) {
        return Optional.ofNullable(solrIndexStore.readDocument(docRef)).map(SolrIndexDoc::getDescription);
    }

    @Override
    public DocRef fetchDefaultExtractionPipeline(final DocRef dataSourceRef) {
        return securityContext.useAsReadResult(() -> {
            final SolrIndexDoc index = solrIndexStore.readDocument(dataSourceRef);
            if (index != null) {
                return index.getDefaultExtractionPipeline();
            }
            return null;
        });
    }

    @Override
    public QueryField getTimeField(final DocRef docRef) {
        return securityContext.useAsReadResult(() -> {
            final SolrIndexDoc index = solrIndexStore.readDocument(docRef);
            QueryField timeField = null;
            if (index.getTimeField() != null && !index.getTimeField().isBlank()) {
                return QueryField.createDate(index.getTimeField());
            }

            return null;
        });
    }

    @Override
    public ResultStore createResultStore(final SearchRequest searchRequest) {
        // Replace expression parameters.
        final SearchRequest modifiedSearchRequest = ExpressionUtil.replaceExpressionParameters(searchRequest);

        // Get the search.
        final Query query = modifiedSearchRequest.getQuery();

        // Extract highlights.
        final Set<String> highlights = getHighlights(
                query.getDataSource(),
                indexFieldCache,
                query.getExpression(),
                modifiedSearchRequest.getDateTimeSettings());

        // Create a coprocessor settings list.
        final List<CoprocessorSettings> coprocessorSettingsList = coprocessorsFactory
                .createSettings(modifiedSearchRequest);

        // Create a handler for search results.
        final CoprocessorsImpl coprocessors = coprocessorsFactory.create(
                modifiedSearchRequest.getSearchRequestSource(),
                modifiedSearchRequest.getDateTimeSettings(),
                modifiedSearchRequest.getKey(),
                coprocessorSettingsList,
                modifiedSearchRequest.getQuery().getParams(),
                DataStoreSettings.createBasicSearchResultStoreSettings());

        // Create an asynchronous search task.
        final String searchName = "Search '" + modifiedSearchRequest.getKey().toString() + "'";
        final SolrAsyncSearchTask asyncSearchTask = new SolrAsyncSearchTask(
                modifiedSearchRequest.getKey(),
                searchName,
                query,
                coprocessorSettingsList,
                modifiedSearchRequest.getDateTimeSettings());

        // Create the search result store.
        final ResultStore resultStore = resultStoreFactory.create(
                searchRequest.getSearchRequestSource(),
                coprocessors);
        resultStore.addHighlights(highlights);

        // Start asynchronous search execution.
        solrSearchExecutor.start(asyncSearchTask, resultStore);

        return resultStore;
    }

    /**
     * Compiles the query, extracts terms and then returns them for use in hit
     * highlighting.
     */
    private Set<String> getHighlights(final DocRef indexDocRef,
                                      final IndexFieldCache indexFieldCache,
                                      final ExpressionOperator expression,
                                      final DateTimeSettings dateTimeSettings) {
        Set<String> highlights = Collections.emptySet();

        try {
            // Parse the query.
            final SearchExpressionQueryBuilder searchExpressionQueryBuilder = new SearchExpressionQueryBuilder(
                    indexDocRef,
                    indexFieldCache,
                    wordListProvider,
                    searchConfig.getMaxBooleanClauseCount(),
                    dateTimeSettings);
            final SearchExpressionQuery query = searchExpressionQueryBuilder
                    .buildQuery(expression);

            highlights = query.getTerms();
        } catch (final RuntimeException e) {
            LOGGER.debug(e.getMessage(), e);
        }

        return highlights;
    }

    @Override
    public List<DocRef> list() {
        return solrIndexStore.list();
    }

    @Override
    public String getType() {
        return SolrIndexDoc.DOCUMENT_TYPE;
    }
}
