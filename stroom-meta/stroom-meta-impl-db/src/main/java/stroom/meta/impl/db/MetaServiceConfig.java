package stroom.meta.impl.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import stroom.config.common.ConnectionConfig;
import stroom.config.common.ConnectionPoolConfig;
import stroom.util.shared.IsConfig;

import javax.inject.Singleton;

@Singleton
public class MetaServiceConfig implements IsConfig {
    private ConnectionConfig connectionConfig = new ConnectionConfig();
    private ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();
    private MetaValueConfig metaValueConfig;

    @JsonProperty("connection")
    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(final ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    @JsonProperty("connectionPool")
    public ConnectionPoolConfig getConnectionPoolConfig() {
        return connectionPoolConfig;
    }

    public void setConnectionPoolConfig(final ConnectionPoolConfig connectionPoolConfig) {
        this.connectionPoolConfig = connectionPoolConfig;
    }

    public MetaValueConfig getMetaValueConfig() {
        return metaValueConfig;
    }

    public void setMetaValueConfig(final MetaValueConfig metaValueConfig) {
        this.metaValueConfig = metaValueConfig;
    }
}
