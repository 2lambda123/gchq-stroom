package stroom.db.util;

import stroom.config.common.AbstractDbConfig;

import java.util.Objects;

public class DataSourceKey {

    private final AbstractDbConfig config;
    private final String name;
    private final boolean unique;

    public DataSourceKey(final AbstractDbConfig config,
                         final String name,
                         final boolean unique) {
        this.config = config;
        this.name = name;
        this.unique = unique;
    }

    public AbstractDbConfig getConfig() {
        return config;
    }

    public String getPoolName() {
        return "hikari-" + name;
    }

    /**
     * WARNING: THIS IS A SPECIAL EQUALS METHOD
     * <p>
     * This method is specifically designed to only include the data source name in the equality if we want to force a
     * unique data source to be created based on the config plus the name. In most cases we dedupe the datasource on the
     * connection config alone and don't use the name.
     *
     * @param o The object to compare
     * @return True if the objects are the same.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DataSourceKey key = (DataSourceKey) o;
        if (unique) {
            return config.equals(key.config) && name.equals(key.name);
        }
        return config.equals(key.config);
    }

    /**
     * WARNING: THIS IS A SPECIAL HASHCODE METHOD
     * <p>
     * This method is specifically designed to only include the data source name in the hashcode if we want to force a
     * unique data source to be created based on the config plus the name. In most cases we dedupe the datasource on the
     * connection config alone and don't use the name.
     *
     * @return The hashcode.
     */
    @Override
    public int hashCode() {
        if (unique) {
            return Objects.hash(config, name);
        }
        return Objects.hash(config);
    }

    @Override
    public String toString() {
        return "Key{" +
                "config=" + config +
                ", name='" + name + '\'' +
                ", unique=" + unique +
                '}';
    }
}
