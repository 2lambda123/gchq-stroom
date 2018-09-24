package stroom.persist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import stroom.config.common.ConnectionConfig;
import stroom.config.common.ConnectionPoolConfig;
import stroom.util.config.annotations.ReadOnly;
import stroom.util.io.FileUtil;

import javax.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;

@Singleton
public class CoreConfig {
    private ConnectionConfig connectionConfig = new ConnectionConfig();
    private ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();
    private HibernateConfig hibernateConfig = new HibernateConfig();
    private String temp;
    private int databaseMultiInsertMaxBatchSize = 500;

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

    @JsonProperty("hibernate")
    public HibernateConfig getHibernateConfig() {
        return hibernateConfig;
    }

    public void setHibernateConfig(final HibernateConfig hibernateConfig) {
        this.hibernateConfig = hibernateConfig;
    }

    @ReadOnly
    @JsonPropertyDescription("Temp folder to write stuff to. Should only be set per node in application property file")
    public String getTemp() {
        return temp;
    }

    public void setTemp(final String temp) {
        this.temp = temp;

        if (temp != null) {
            final Path tempDir = Paths.get(temp);
            FileUtil.setTempDir(tempDir);
        }
    }

    @JsonPropertyDescription("The maximum number of rows to insert in a single multi insert statement, e.g. INSERT INTO X VALUES (...), (...), (...)")
    public int getDatabaseMultiInsertMaxBatchSize() {
        return databaseMultiInsertMaxBatchSize;
    }

    public void setDatabaseMultiInsertMaxBatchSize(final int databaseMultiInsertMaxBatchSize) {
        this.databaseMultiInsertMaxBatchSize = databaseMultiInsertMaxBatchSize;
    }

    @Override
    public String toString() {
        return "CoreConfig{" +
                "temp='" + temp + '\'' +
                ", databaseMultiInsertMaxBatchSize=" + databaseMultiInsertMaxBatchSize +
                '}';
    }
}
