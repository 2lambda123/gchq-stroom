package stroom.cluster.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import stroom.util.shared.IsConfig;
import stroom.util.config.annotations.RequiresRestart;
import stroom.util.shared.ModelStringUtil;

import javax.inject.Singleton;

@Singleton
public class ClusterConfig implements IsConfig {
    private boolean clusterCallUseLocal = true;
    private String clusterCallReadTimeout = "30s";
    private boolean clusterCallIgnoreSSLHostnameVerifier = true;
    private String clusterResponseTimeout = "30s";

    @RequiresRestart(RequiresRestart.RestartScope.SYSTEM)
    @JsonPropertyDescription("Do local calls when calling our own local services (true is an optimisation)")
    public boolean isClusterCallUseLocal() {
        return clusterCallUseLocal;
    }

    public void setClusterCallUseLocal(final boolean clusterCallUseLocal) {
        this.clusterCallUseLocal = clusterCallUseLocal;
    }

    @RequiresRestart(RequiresRestart.RestartScope.SYSTEM)
    @JsonPropertyDescription("Time in ms (but can be specified as 10s, 1m) before throwing read timeout")
    public String getClusterCallReadTimeout() {
        return clusterCallReadTimeout;
    }

    public void setClusterCallReadTimeout(final String clusterCallReadTimeout) {
        this.clusterCallReadTimeout = clusterCallReadTimeout;
    }

    @RequiresRestart(RequiresRestart.RestartScope.SYSTEM)
    @JsonPropertyDescription("If cluster calls are using SSL then choose if we want to ignore host name " +
            "verification")
    public boolean isClusterCallIgnoreSSLHostnameVerifier() {
        return clusterCallIgnoreSSLHostnameVerifier;
    }

    public void setClusterCallIgnoreSSLHostnameVerifier(final boolean clusterCallIgnoreSSLHostnameVerifier) {
        this.clusterCallIgnoreSSLHostnameVerifier = clusterCallIgnoreSSLHostnameVerifier;
    }

    @JsonPropertyDescription("Time in ms (but can be specified as 10s, 1m) before giving up on cluster results")
    public String getClusterResponseTimeout() {
        return clusterResponseTimeout;
    }

    public void setClusterResponseTimeout(final String clusterResponseTimeout) {
        this.clusterResponseTimeout = clusterResponseTimeout;
    }

    @JsonIgnore
    public long getClusterCallReadTimeoutMs() {
        return ModelStringUtil.parseDurationString(clusterCallReadTimeout);
    }

    @JsonIgnore
    public long getClusterResponseTimeoutMs() {
        return ModelStringUtil.parseDurationString(clusterResponseTimeout);
    }

    @Override
    public String toString() {
        return "ClusterConfig{" +
                "clusterCallUseLocal=" + clusterCallUseLocal +
                ", clusterCallReadTimeout='" + clusterCallReadTimeout + '\'' +
                ", clusterCallIgnoreSSLHostnameVerifier=" + clusterCallIgnoreSSLHostnameVerifier +
                ", clusterResponseTimeout='" + clusterResponseTimeout + '\'' +
                '}';
    }
}
