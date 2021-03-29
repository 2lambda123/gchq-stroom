package stroom.index.impl;

import stroom.config.common.DbConfig;
import stroom.config.common.HasDbConfig;
import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import javax.inject.Singleton;

@Singleton
public class IndexConfig extends AbstractConfig implements HasDbConfig {

    private DbConfig dbConfig = new DbConfig();
    private int ramBufferSizeMB = 1024;
    private IndexWriterConfig indexWriterConfig = new IndexWriterConfig();
    private CacheConfig indexStructureCache = CacheConfig.builder()
            .maximumSize(100L)
            .expireAfterWrite(StroomDuration.ofSeconds(10))
            .build();

    @JsonProperty("db")
    public DbConfig getDbConfig() {
        return dbConfig;
    }

    public void setDbConfig(final DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @JsonPropertyDescription("The amount of RAM Lucene can use to buffer when indexing in Mb")
    public int getRamBufferSizeMB() {
        return ramBufferSizeMB;
    }

    public void setRamBufferSizeMB(final int ramBufferSizeMB) {
        this.ramBufferSizeMB = ramBufferSizeMB;
    }

    @JsonProperty("writer")
    public IndexWriterConfig getIndexWriterConfig() {
        return indexWriterConfig;
    }

    public void setIndexWriterConfig(final IndexWriterConfig indexWriterConfig) {
        this.indexWriterConfig = indexWriterConfig;
    }

    public CacheConfig getIndexStructureCache() {
        return indexStructureCache;
    }

    public void setIndexStructureCache(final CacheConfig indexStructureCache) {
        this.indexStructureCache = indexStructureCache;
    }

    @Override
    public String toString() {
        return "IndexConfig{" +
                "dbConfig=" + dbConfig +
                ", ramBufferSizeMB=" + ramBufferSizeMB +
                ", indexWriterConfig=" + indexWriterConfig +
                ", indexStructureCache=" + indexStructureCache +
                '}';
    }
}
