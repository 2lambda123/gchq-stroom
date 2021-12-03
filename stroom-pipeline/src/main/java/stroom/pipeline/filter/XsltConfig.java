package stroom.pipeline.filter;

import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;


public class XsltConfig extends AbstractConfig {

    private static final int DEFAULT_MAX_ELEMENTS = 1000000;

    private final CacheConfig cacheConfig;
    private final int maxElements;

    public XsltConfig() {
        cacheConfig = CacheConfig.builder()
                .maximumSize(1000L)
                .expireAfterAccess(StroomDuration.ofMinutes(10))
                .build();
        maxElements = DEFAULT_MAX_ELEMENTS;
    }

    @SuppressWarnings("unused")
    @JsonCreator
    public XsltConfig(@JsonProperty("cache") final CacheConfig cacheConfig,
                      @JsonProperty("maxElements") final int maxElements) {
        this.cacheConfig = cacheConfig;
        this.maxElements = maxElements;
    }

    @JsonProperty("cache")
    @JsonPropertyDescription("The cache config for the XSLT pool.")
    public CacheConfig getCacheConfig() {
        return cacheConfig;
    }

    @JsonPropertyDescription("The maximum number of elements that the XSLT filter will expect to receive before " +
            "it errors. This protects Stroom from running out of memory in cases where an appropriate XML splitter " +
            "has not been used in a pipeline.")
    public int getMaxElements() {
        return maxElements;
    }

    @Override
    public String toString() {
        return "XsltConfig{" +
                "cacheConfig=" + cacheConfig +
                ", maxElements=" + maxElements +
                '}';
    }
}
