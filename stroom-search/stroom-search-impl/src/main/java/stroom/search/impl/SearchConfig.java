package stroom.search.impl;

import stroom.query.common.v2.ResultStoreConfig;
import stroom.search.extraction.ExtractionConfig;
import stroom.search.impl.shard.IndexShardSearchConfig;
import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;


public class SearchConfig extends AbstractConfig {

    /**
     * We don't want to collect more than 10k doc's data into the queue by
     * default. When the queue is full the index shard data tasks will pause
     * until the docs are drained from the queue.
     */
    private static final int DEFAULT_MAX_STORED_DATA_QUEUE_SIZE = 1000;
    private static final int DEFAULT_MAX_BOOLEAN_CLAUSE_COUNT = 1024;

    private int maxStoredDataQueueSize = DEFAULT_MAX_STORED_DATA_QUEUE_SIZE;
    private int maxBooleanClauseCount = DEFAULT_MAX_BOOLEAN_CLAUSE_COUNT;
    private String storeSize = "1000000,100,10,1";
    private ExtractionConfig extractionConfig = new ExtractionConfig();
    private IndexShardSearchConfig shardConfig = new IndexShardSearchConfig();
    private ResultStoreConfig resultStoreConfig = new ResultStoreConfig();

    private CacheConfig resultStoreCache = CacheConfig.builder()
            .maximumSize(100L)
            .expireAfterAccess(StroomDuration.ofMinutes(1))
            .build();
    private CacheConfig remoteSearchResultCache = CacheConfig.builder()
            .maximumSize(100L)
            .expireAfterAccess(StroomDuration.ofMinutes(1))
            .build();

    @JsonPropertyDescription("The maximum number documents that will have stored data retrieved from the index " +
            "shard and queued prior to further processing")
    public int getMaxStoredDataQueueSize() {
        return maxStoredDataQueueSize;
    }

    public void setMaxStoredDataQueueSize(final int maxStoredDataQueueSize) {
        this.maxStoredDataQueueSize = maxStoredDataQueueSize;
    }

    @JsonPropertyDescription("The maximum number of clauses that a boolean search can contain.")
    public int getMaxBooleanClauseCount() {
        return maxBooleanClauseCount;
    }

    public void setMaxBooleanClauseCount(final int maxBooleanClauseCount) {
        this.maxBooleanClauseCount = maxBooleanClauseCount;
    }

    @JsonPropertyDescription("The maximum number of search results to keep in memory at each level.")
    public String getStoreSize() {
        return storeSize;
    }

    public void setStoreSize(final String storeSize) {
        this.storeSize = storeSize;
    }

    @JsonProperty("extraction")
    public ExtractionConfig getExtractionConfig() {
        return extractionConfig;
    }

    public void setExtractionConfig(final ExtractionConfig extractionConfig) {
        this.extractionConfig = extractionConfig;
    }

    @JsonProperty("shard")
    public IndexShardSearchConfig getShardConfig() {
        return shardConfig;
    }

    public void setShardConfig(final IndexShardSearchConfig shardConfig) {
        this.shardConfig = shardConfig;
    }

    public CacheConfig getResultStoreCache() {
        return resultStoreCache;
    }

    public void setResultStoreCache(final CacheConfig resultStoreCache) {
        this.resultStoreCache = resultStoreCache;
    }

    @JsonProperty("remoteSearchResultCache")
    public CacheConfig getRemoteSearchResultCache() {
        return remoteSearchResultCache;
    }

    public void setRemoteSearchResultCache(final CacheConfig remoteSearchResultCache) {
        this.remoteSearchResultCache = remoteSearchResultCache;
    }

    @JsonProperty("resultStore")
    public ResultStoreConfig getLmdbConfig() {
        return resultStoreConfig;
    }

    public void setLmdbConfig(final ResultStoreConfig resultStoreConfig) {
        this.resultStoreConfig = resultStoreConfig;
    }

    @Override
    public String toString() {
        return "SearchConfig{" +
                "maxStoredDataQueueSize=" + maxStoredDataQueueSize +
                ", maxBooleanClauseCount=" + maxBooleanClauseCount +
                ", storeSize='" + storeSize + '\'' +
                '}';
    }
}
