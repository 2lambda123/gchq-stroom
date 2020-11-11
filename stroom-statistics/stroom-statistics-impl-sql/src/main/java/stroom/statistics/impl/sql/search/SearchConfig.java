package stroom.statistics.impl.sql.search;

import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import javax.inject.Singleton;

@Singleton
public class SearchConfig extends AbstractConfig {

    private String storeSize = "1000000,100,10,1";
    private int maxResults = 100000;
    private int fetchSize = 5000;
    private CacheConfig searchResultCache = new CacheConfig.Builder()
            .maximumSize(10000L)
            .expireAfterAccess(StroomDuration.ofMinutes(10))
            .build();

    @JsonPropertyDescription("The maximum number of search results to keep in memory at each level.")
    public String getStoreSize() {
        return storeSize;
    }

    public void setStoreSize(final String storeSize) {
        this.storeSize = storeSize;
    }

    @JsonPropertyDescription("The maximum number of records that can be returned from the statistics DB in a single query prior to aggregation")
    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(final int maxResults) {
        this.maxResults = maxResults;
    }

    @JsonPropertyDescription("Gives the JDBC driver a hint as to the number of rows that should be fetched from the database when more rows are needed for ResultSet objects generated by this Statement. Depends on 'useCursorFetch=true' being set in the JDBC connect string. If not set, the JDBC driver's default will be used.")
    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(final int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public CacheConfig getSearchResultCache() {
        return searchResultCache;
    }

    public void setSearchResultCache(final CacheConfig searchResultCache) {
        this.searchResultCache = searchResultCache;
    }

    @Override
    public String toString() {
        return "SearchConfig{" +
                "storeSize='" + storeSize + '\'' +
                ", maxResults=" + maxResults +
                ", fetchSize=" + fetchSize +
                ", searchResultCache=" + searchResultCache +
                '}';
    }
}
