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

package stroom.search.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import stroom.dictionary.server.DictionaryStore;
import stroom.index.server.IndexService;
import stroom.index.server.LuceneVersionUtil;
import stroom.index.shared.Index;
import stroom.index.shared.IndexFieldsMap;
import stroom.node.server.NodeCache;
import stroom.node.server.StroomPropertyService;
import stroom.node.shared.ClientProperties;
import stroom.node.shared.Node;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.Query;
import stroom.query.api.v2.SearchRequest;
import stroom.query.common.v2.CoprocessorSettingsMap;
import stroom.query.common.v2.SearchResultHandler;
import stroom.query.common.v2.Sizes;
import stroom.query.common.v2.Store;
import stroom.query.common.v2.StoreFactory;
import stroom.search.server.SearchExpressionQueryBuilder.SearchExpressionQuery;
import stroom.security.SecurityContext;
import stroom.security.SecurityHelper;
import stroom.task.cluster.ClusterResultCollectorCache;
import stroom.task.server.TaskManager;
import stroom.util.config.PropertyUtil;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;


@SuppressWarnings("unused") //used by DI
@Component("luceneSearchStoreFactory")
public class LuceneSearchStoreFactory implements StoreFactory {
    public static final String ENTITY_TYPE = Index.ENTITY_TYPE;
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneSearchStoreFactory.class);
    private static final int SEND_INTERACTIVE_SEARCH_RESULT_FREQUENCY = 500;
    private static final int DEFAULT_MAX_BOOLEAN_CLAUSE_COUNT = 1024;

    private final IndexService indexService;
    private final DictionaryStore dictionaryStore;
    private final StroomPropertyService stroomPropertyService;
    private final NodeCache nodeCache;
    private final TaskManager taskManager;
    private final ClusterResultCollectorCache clusterResultCollectorCache;
    private final int maxBooleanClauseCount;
    private final SecurityContext securityContext;

    @Inject
    public LuceneSearchStoreFactory(final IndexService indexService,
                                    final DictionaryStore dictionaryStore,
                                    final StroomPropertyService stroomPropertyService,
                                    final NodeCache nodeCache,
                                    final TaskManager taskManager,
                                    final ClusterResultCollectorCache clusterResultCollectorCache,
                                    @Value("#{propertyConfigurer.getProperty('stroom.search.maxBooleanClauseCount')}") final String maxBooleanClauseCount,
                                    final SecurityContext securityContext) {
        this.indexService = indexService;
        this.dictionaryStore = dictionaryStore;
        this.stroomPropertyService = stroomPropertyService;
        this.nodeCache = nodeCache;
        this.taskManager = taskManager;
        this.clusterResultCollectorCache = clusterResultCollectorCache;
        this.maxBooleanClauseCount = PropertyUtil.toInt(maxBooleanClauseCount, DEFAULT_MAX_BOOLEAN_CLAUSE_COUNT);
        this.securityContext = securityContext;
    }

    @Override
    public Store create(final SearchRequest searchRequest) {
        // Get the current time in millis since epoch.
        final long nowEpochMilli = System.currentTimeMillis();

        // Get the search.
        final Query query = searchRequest.getQuery();

        // Load the index.
        final Index index;
        try (final SecurityHelper securityHelper = SecurityHelper.elevate(securityContext)) {
            index = indexService.loadByUuid(query.getDataSource().getUuid());
        }

        // Extract highlights.
        final Set<String> highlights = getHighlights(index, query.getExpression(), searchRequest.getDateTimeLocale(), nowEpochMilli);

        // This is a new search so begin a new asynchronous search.
        final Node node = nodeCache.getDefaultNode();

        // Create a coprocessor settings map.
        final CoprocessorSettingsMap coprocessorSettingsMap = CoprocessorSettingsMap.create(searchRequest);

        // Create an asynchronous search task.
        final String searchName = "Search '" + searchRequest.getKey().toString() + "'";
        final AsyncSearchTask asyncSearchTask = new AsyncSearchTask(
                null,
                securityContext.getUserIdentity(),
                searchName,
                query,
                node,
                SEND_INTERACTIVE_SEARCH_RESULT_FREQUENCY,
                coprocessorSettingsMap.getMap(),
                searchRequest.getDateTimeLocale(),
                nowEpochMilli);

        // Create a handler for search results.
        final Sizes storeSize = getStoreSizes();
        final Sizes defaultMaxResultsSizes = getDefaultMaxResultsSizes();
        final SearchResultHandler resultHandler = new SearchResultHandler(
                coprocessorSettingsMap,
                defaultMaxResultsSizes,
                storeSize);

        // Create the search result collector.
        final ClusterSearchResultCollector searchResultCollector = ClusterSearchResultCollector.create(
                taskManager,
                asyncSearchTask,
                node,
                highlights,
                clusterResultCollectorCache,
                resultHandler,
                defaultMaxResultsSizes,
                storeSize);

        // Tell the task where results will be collected.
        asyncSearchTask.setResultCollector(searchResultCollector);

        // Start asynchronous search execution.
        searchResultCollector.start();

        return searchResultCollector;
    }

    private Sizes getDefaultMaxResultsSizes() {
        final String value = stroomPropertyService.getProperty(ClientProperties.DEFAULT_MAX_RESULTS);
        return extractValues(value);
    }

    private Sizes getStoreSizes() {
        final String value = stroomPropertyService.getProperty(ClusterSearchResultCollector.PROP_KEY_STORE_SIZE);
        return extractValues(value);
    }

    private Sizes extractValues(String value) {
        if (value != null) {
            try {
                return Sizes.create(Arrays.stream(value.split(","))
                        .map(String::trim)
                        .map(Integer::valueOf)
                        .collect(Collectors.toList()));
            } catch (Exception e) {
                LOGGER.warn(e.getMessage());
            }
        }
        return Sizes.create(Integer.MAX_VALUE);
    }

    /**
     * Compiles the query, extracts terms and then returns them for use in hit
     * highlighting.
     */
    private Set<String> getHighlights(final Index index, final ExpressionOperator expression, final String timeZoneId, final long nowEpochMilli) {
        Set<String> highlights = Collections.emptySet();

        try {
            // Create a map of index fields keyed by name.
            final IndexFieldsMap indexFieldsMap = new IndexFieldsMap(index.getIndexFieldsObject());
            // Parse the query.
            final SearchExpressionQueryBuilder searchExpressionQueryBuilder = new SearchExpressionQueryBuilder(
                    dictionaryStore, indexFieldsMap, maxBooleanClauseCount, timeZoneId, nowEpochMilli);
            final SearchExpressionQuery query = searchExpressionQueryBuilder
                    .buildQuery(LuceneVersionUtil.CURRENT_LUCENE_VERSION, expression);

            highlights = query.getTerms();
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }

        return highlights;
    }
}
