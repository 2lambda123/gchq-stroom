package stroom.dashboard.impl;

import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.time.StroomDuration;

import javax.inject.Singleton;

@Singleton
public class DashboardConfig extends AbstractConfig {

    private CacheConfig activeQueriesCache = CacheConfig.builder()
            .maximumSize(100L)
            .expireAfterAccess(StroomDuration.ofMinutes(1))
            .build();

    public CacheConfig getActiveQueriesCache() {
        return activeQueriesCache;
    }

    @SuppressWarnings("unused")
    public void setActiveQueriesCache(final CacheConfig activeQueriesCache) {
        this.activeQueriesCache = activeQueriesCache;
    }

    @Override
    public String toString() {
        return "DashboardConfig{" +
                "activeQueriesCache=" + activeQueriesCache +
                '}';
    }
}
