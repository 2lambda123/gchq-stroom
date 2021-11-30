package stroom.core.receive;

import stroom.data.zip.BufferSizeUtil;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.ModelStringUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import javax.inject.Singleton;

public class ProxyAggregationConfig extends AbstractConfig {

    private String proxyDir = "proxy_repo";
    private volatile int proxyThreads = 10;

    private volatile int maxFileScan = 100000;
    private volatile int maxConcurrentMappedFiles = 100000;
    private volatile int maxFilesPerAggregate = 10000;
    private String maxUncompressedFileSize = "1G";

    @JsonPropertyDescription("Directory to look for Stroom Proxy Content to aggregate. Typically this directory " +
            "will belong to the stroom-proxy that is populating the repository in it. If the value is a " +
            "relative path then it will be treated as being relative to stroom.path.home.")
    public String getProxyDir() {
        return proxyDir;
    }

    public void setProxyDir(final String proxyDir) {
        this.proxyDir = proxyDir;
    }

    @JsonPropertyDescription("The amount of memory to use for buffering reads/writes")
    public int getBuffferSize() {
        return BufferSizeUtil.get();
    }

    public void setBuffferSize(final int buffferSize) {
        BufferSizeUtil.setValue(buffferSize);
    }

    @JsonPropertyDescription("Number of threads used in aggregation")
    public int getProxyThreads() {
        return proxyThreads;
    }

    public void setProxyThreads(final int proxyThreads) {
        this.proxyThreads = proxyThreads;
    }

    @JsonPropertyDescription("The limit of files to inspect before aggregation begins (should be bigger than " +
            "maxAggregation)")
    public int getMaxFileScan() {
        return maxFileScan;
    }

    public void setMaxFileScan(final int maxFileScan) {
        this.maxFileScan = maxFileScan;
    }

    @JsonPropertyDescription("The maximum number of file references in aggregation file sets to hold in memory " +
            "prior to aggregation")
    public int getMaxConcurrentMappedFiles() {
        return maxConcurrentMappedFiles;
    }

    public void setMaxConcurrentMappedFiles(final int maxConcurrentMappedFiles) {
        this.maxConcurrentMappedFiles = maxConcurrentMappedFiles;
    }

    @JsonPropertyDescription("The maximum number of files that can be aggregated together")
    public int getMaxFilesPerAggregate() {
        return maxFilesPerAggregate;
    }

    public void setMaxFilesPerAggregate(final int maxFilesPerAggregate) {
        this.maxFilesPerAggregate = maxFilesPerAggregate;
    }

    @JsonPropertyDescription("The maximum total size of the uncompressed contents that will be held in an " +
            "aggregate unless the first and only aggregated file exceeds this limit")
    public String getMaxUncompressedFileSize() {
        return maxUncompressedFileSize;
    }

    public void setMaxUncompressedFileSize(final String maxUncompressedFileSize) {
        this.maxUncompressedFileSize = maxUncompressedFileSize;
    }

    @JsonIgnore
    public long getMaxUncompressedFileSizeBytes() {
        return ModelStringUtil.parseIECByteSizeString(maxUncompressedFileSize);
    }

    @Override
    public String toString() {
        return "ProxyAggregationConfig{" +
                "proxyDir='" + proxyDir + '\'' +
                ", proxyThreads=" + proxyThreads +
                ", maxFilesPerAggregate=" + maxFilesPerAggregate +
                ", maxConcurrentMappedFiles=" + maxConcurrentMappedFiles +
                ", maxUncompressedFileSize='" + maxUncompressedFileSize + '\'' +
                '}';
    }
}
