package stroom.index.lucene980;

import stroom.index.lucene980.SearchExpressionQueryBuilder.SearchExpressionQuery;
import stroom.index.lucene980.analyser.AnalyzerFactory;
import stroom.index.shared.IndexFieldCache;
import stroom.index.shared.LuceneIndexField;
import stroom.query.api.v2.SearchRequest;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import jakarta.inject.Inject;
import org.apache.lucene980.analysis.Analyzer;

import java.util.HashMap;
import java.util.Map;

class SearchExpressionQueryCache {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SearchExpressionQueryCache.class);

    private final SearchExpressionQueryBuilderFactory searchExpressionQueryBuilderFactory;
    private final IndexFieldCache indexFieldCache;
    private final Map<String, Analyzer> analyzerMap = new HashMap<>();
    private SearchExpressionQuery luceneQuery;

    @Inject
    SearchExpressionQueryCache(final SearchExpressionQueryBuilderFactory searchExpressionQueryBuilderFactory,
                               final IndexFieldCache indexFieldCache) {
        this.searchExpressionQueryBuilderFactory = searchExpressionQueryBuilderFactory;
        this.indexFieldCache = indexFieldCache;
    }

    SearchExpressionQuery getQuery(final SearchRequest searchRequest,
                                   final boolean ignoreMissingFields) {
        try {
            if (luceneQuery == null) {
                final SearchExpressionQueryBuilder searchExpressionQueryBuilder =
                        searchExpressionQueryBuilderFactory.create(
                                searchRequest.getQuery().getDataSource(),
                                indexFieldCache,
                                searchRequest.getDateTimeSettings());
                luceneQuery = searchExpressionQueryBuilder
                        .buildQuery(searchRequest.getQuery().getExpression());
            }
            return luceneQuery;
        } catch (final RuntimeException e) {
            LOGGER.error(e::getMessage, e);
            throw e;
        }
    }

    Analyzer getAnalyser(final LuceneIndexField indexField) {
        try {
            Analyzer fieldAnalyzer = analyzerMap.get(indexField.getFldName());
            if (fieldAnalyzer == null) {
                // Add the field analyser.
                fieldAnalyzer = AnalyzerFactory.create(indexField.getAnalyzerType(),
                        indexField.isCaseSensitive());
                analyzerMap.put(indexField.getFldName(), fieldAnalyzer);
            }
            return fieldAnalyzer;
        } catch (final RuntimeException e) {
            LOGGER.error(e::getMessage, e);
            throw e;
        }
    }
}
