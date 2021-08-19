package stroom.pipeline.filter;

import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.IsStroomConfig;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import javax.inject.Singleton;

@Singleton
public class XsltConfig extends AbstractConfig implements IsStroomConfig {

    private static final int DEFAULT_MAX_ELEMENTS = 1000000;

    private CacheConfig cacheConfig = CacheConfig.builder()
            .maximumSize(1000L)
            .expireAfterAccess(StroomDuration.ofMinutes(10))
            .build();
    private int maxElements = DEFAULT_MAX_ELEMENTS;

    @JsonProperty("cache")
    @JsonPropertyDescription("The cache config for the XSLT pool.")
    public CacheConfig getCacheConfig() {
        return cacheConfig;
    }

    public void setCacheConfig(final CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
    }

    @JsonPropertyDescription("The maximum number of elements that the XSLT filter will expect to receive before " +
            "it errors. This protects Stroom from running out of memory in cases where an appropriate XML splitter " +
            "has not been used in a pipeline.")
    public int getMaxElements() {
        return maxElements;
    }

    public void setMaxElements(final int maxElements) {
        this.maxElements = maxElements;
    }

    @Override
    public String toString() {
        return "XsltConfig{" +
                "cacheConfig=" + cacheConfig +
                ", maxElements=" + maxElements +
                '}';
    }
}
