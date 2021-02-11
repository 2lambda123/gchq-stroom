package stroom.db.util;

import stroom.config.common.ConnectionConfig;
import stroom.config.common.ConnectionPoolConfig;
import stroom.config.common.DbConfig;
import stroom.util.time.StroomDuration;

import com.zaxxer.hikari.HikariConfig;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class HikariUtil {

    private HikariUtil() {
        // Utility class.
    }

    public static HikariConfig createConfig(final DbConfig dbConfig) {
        final ConnectionConfig connectionConfig = dbConfig.getConnectionConfig();
        final ConnectionPoolConfig connectionPoolConfig = dbConfig.getConnectionPoolConfig();

        // Validate the connection details.
        DbUtil.validate(connectionConfig);
        // Keep waiting until we can establish a DB connection to allow for the DB to start after the app
        DbUtil.waitForConnection(connectionConfig);

        return create(connectionConfig, connectionPoolConfig);
    }

    private static HikariConfig create(final ConnectionConfig connectionConfig,
            final ConnectionPoolConfig connectionPoolConfig) {
        final HikariConfig config = new HikariConfig();

        // Pool properties
        copyAndMapProp(connectionPoolConfig::getConnectionTimeout,
                config::setConnectionTimeout,
                StroomDuration::toMillis);
        copyAndMapProp(connectionPoolConfig::getIdleTimeout, config::setIdleTimeout, StroomDuration::toMillis);
        copyAndMapProp(connectionPoolConfig::getMaxLifetime, config::setMaxLifetime, StroomDuration::toMillis);
        copyAndMapProp(connectionPoolConfig::getMinimumIdle, config::setMinimumIdle, Integer::intValue);
        copyAndMapProp(connectionPoolConfig::getMaxPoolSize, config::setMaximumPoolSize, Integer::intValue);

        copyAndMapProp(connectionConfig::getUrl, config::setJdbcUrl, Function.identity());
        copyAndMapProp(connectionConfig::getUser, config::setUsername, Function.identity());
        copyAndMapProp(connectionConfig::getPassword, config::setPassword, Function.identity());

        // JDBC Driver properties
        copyAndMapProp(connectionPoolConfig::getCachePrepStmts,
                val -> config.addDataSourceProperty("cachePrepStmts", val),
                String::valueOf);
        copyAndMapProp(connectionPoolConfig::getPrepStmtCacheSize,
                val -> config.addDataSourceProperty("prepStmtCacheSize", val),
                String::valueOf);
        copyAndMapProp(connectionPoolConfig::getPrepStmtCacheSqlLimit,
                val -> config.addDataSourceProperty("prepStmtCacheSqlLimit", val),
                String::valueOf);

        return config;
    }

    /**
     * If the source supplier (i.e. getter) has a non-null value then set it
     * on the dest consumer (i.e. setter). Convert the type of the value in the
     * process.
     */
    private static <T1, T2> void copyAndMapProp(final Supplier<T1> source,
            final Consumer<T2> dest,
            final Function<T1, T2> typeMapper) {
        final T1 sourceValue = source.get();
        if (sourceValue != null) {
            dest.accept(typeMapper.apply(sourceValue));
        }
    }
}
