package stroom.index.lucene553;

import stroom.dictionary.api.WordListProvider;
import stroom.docref.DocRef;
import stroom.expression.api.DateTimeSettings;
import stroom.index.impl.IndexShardSearchConfig;
import stroom.index.impl.IndexShardWriterCache;
import stroom.index.impl.LuceneShardSearcher;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.QueryKey;
import stroom.query.common.v2.IndexFieldCache;
import stroom.search.impl.SearchConfig;
import stroom.task.api.ExecutorProvider;
import stroom.task.api.TaskContextFactory;
import stroom.util.io.PathCreator;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

class Lucene553ShardSearcherFactory {

    private final IndexShardWriterCache indexShardWriterCache;
    private final Provider<IndexShardSearchConfig> shardSearchConfigProvider;
    private final ExecutorProvider executorProvider;
    private final TaskContextFactory taskContextFactory;
    private final PathCreator pathCreator;
    private final WordListProvider dictionaryStore;
    private final Provider<SearchConfig> searchConfigProvider;

    @Inject
    Lucene553ShardSearcherFactory(final IndexShardWriterCache indexShardWriterCache,
                                  final Provider<IndexShardSearchConfig> shardSearchConfigProvider,
                                  final ExecutorProvider executorProvider,
                                  final TaskContextFactory taskContextFactory,
                                  final PathCreator pathCreator,
                                  final WordListProvider dictionaryStore,
                                  final Provider<SearchConfig> searchConfigProvider) {
        this.indexShardWriterCache = indexShardWriterCache;
        this.shardSearchConfigProvider = shardSearchConfigProvider;
        this.executorProvider = executorProvider;
        this.taskContextFactory = taskContextFactory;
        this.pathCreator = pathCreator;
        this.dictionaryStore = dictionaryStore;
        this.searchConfigProvider = searchConfigProvider;
    }

    public LuceneShardSearcher create(final DocRef indexDocRef,
                                      final IndexFieldCache indexFieldCache,
                                      final ExpressionOperator expression,
                                      final DateTimeSettings dateTimeSettings,
                                      final QueryKey queryKey) {
        return new Lucene553ShardSearcher(
                indexShardWriterCache,
                shardSearchConfigProvider.get(),
                executorProvider,
                taskContextFactory,
                pathCreator,
                indexDocRef,
                indexFieldCache,
                expression,
                dictionaryStore,
                searchConfigProvider.get().getMaxBooleanClauseCount(),
                dateTimeSettings,
                queryKey);
    }
}
