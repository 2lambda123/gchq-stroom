package stroom.search.elastic.suggest;

import stroom.datasource.api.v2.FieldInfo;
import stroom.datasource.api.v2.FieldType;
import stroom.query.shared.FetchSuggestionsRequest;
import stroom.query.shared.Suggestions;
import stroom.query.util.LambdaLogger;
import stroom.query.util.LambdaLoggerFactory;
import stroom.search.elastic.ElasticClientCache;
import stroom.search.elastic.ElasticClusterStore;
import stroom.search.elastic.ElasticIndexStore;
import stroom.search.elastic.shared.ElasticClusterDoc;
import stroom.search.elastic.shared.ElasticIndexDoc;
import stroom.task.api.TaskContextFactory;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestion;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder.SuggestMode;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class ElasticSuggestionsQueryHandlerImpl implements ElasticSuggestionsQueryHandler {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ElasticSuggestionsQueryHandlerImpl.class);

    private final Provider<ElasticClientCache> elasticClientCacheProvider;
    private final Provider<ElasticClusterStore> elasticClusterStoreProvider;
    private final Provider<ElasticIndexStore> elasticIndexStoreProvider;
    private final Provider<ElasticSuggestConfig> elasticSuggestConfigProvider;
    private final Provider<TaskContextFactory> taskContextFactoryProvider;

    @Inject
    public ElasticSuggestionsQueryHandlerImpl(final Provider<ElasticClientCache> elasticClientCacheProvider,
                                              final Provider<ElasticClusterStore> elasticClusterStoreProvider,
                                              final Provider<ElasticIndexStore> elasticIndexStoreProvider,
                                              final Provider<ElasticSuggestConfig> elasticSuggestConfigProvider,
                                              final Provider<TaskContextFactory> taskContextFactoryProvider) {
        this.elasticClientCacheProvider = elasticClientCacheProvider;
        this.elasticClusterStoreProvider = elasticClusterStoreProvider;
        this.elasticIndexStoreProvider = elasticIndexStoreProvider;
        this.elasticSuggestConfigProvider = elasticSuggestConfigProvider;
        this.taskContextFactoryProvider = taskContextFactoryProvider;
    }

    @Override
    public Suggestions getSuggestions(final FetchSuggestionsRequest request) {
        final ElasticIndexDoc elasticIndex = elasticIndexStoreProvider.get().readDocument(request.getDataSource());
        final ElasticClusterDoc elasticCluster = elasticClusterStoreProvider.get()
                .readDocument(elasticIndex.getClusterRef());

        CompletableFuture<Suggestions> future = CompletableFuture.supplyAsync(taskContextFactoryProvider.get()
                .contextResult("Query suggestions for Elasticsearch index '" + elasticIndex.getName() + "'",
                        taskContext -> elasticClientCacheProvider.get().contextResult(elasticCluster.getConnection(),
                                elasticClient -> querySuggestions(request, elasticIndex, elasticClient)
                        )
                )
        );

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error(() -> "Thread interrupted");
            return Suggestions.EMPTY;
        } catch (ExecutionException e) {
            throw new RuntimeException("Error getting Elasticsearch term suggestions: " + e.getMessage(), e);
        }
    }

    private Suggestions querySuggestions(final FetchSuggestionsRequest request,
                                          final ElasticIndexDoc elasticIndex,
                                          final RestHighLevelClient elasticClient) {
        final FieldInfo field = request.getField();
        final String query = request.getText();

        try {
            if (!elasticSuggestConfigProvider.get().getEnabled() || query == null || query.length() == 0) {
                return Suggestions.EMPTY;
            }
            if (!(FieldType.TEXT.equals(field.getFieldType()) || FieldType.KEYWORD.equals(field.getFieldType()))) {
                // Only generate suggestions for text and keyword fields
                return Suggestions.EMPTY;
            }

            final SuggestionBuilder<TermSuggestionBuilder> termSuggestionBuilder = SuggestBuilders
                    .termSuggestion(field.getFieldName())
                    .suggestMode(SuggestMode.ALWAYS)
                    .minWordLength(3)
                    .text(query);
            final SuggestBuilder suggestBuilder = new SuggestBuilder()
                    .addSuggestion("suggest", termSuggestionBuilder);
            final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .fetchField(field.getFieldName())
                    .suggest(suggestBuilder);
            final SearchRequest searchRequest = new SearchRequest(elasticIndex.getIndexName())
                    .source(searchSourceBuilder);

            SearchResponse searchResponse = elasticClient.search(searchRequest,
                    RequestOptions.DEFAULT);
            Suggest suggestResponse = searchResponse.getSuggest();
            TermSuggestion termSuggestion = suggestResponse.getSuggestion("suggest");

            return new Suggestions(termSuggestion.getEntries().stream()
                    .flatMap(entry -> entry.getOptions().stream())
                    .map(option -> option.getText().string())
                    .collect(Collectors.toList()));
        } catch (IOException | RuntimeException e) {
            LOGGER.error(() -> "Failed to retrieve search suggestions for field: " + field.getFieldName() +
                    ". " + e.getMessage(), e);
            return Suggestions.EMPTY;
        }
    }
}
