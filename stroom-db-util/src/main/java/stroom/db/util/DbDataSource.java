package stroom.db.util;

import java.util.Objects;
import javax.sql.DataSource;

public class DbDataSource {

    private final DbUrl dbUrl;
    private final DataSource dataSource;

    public DbDataSource(final DbUrl dbUrl,
                        final DataSource dataSource) {
        this.dbUrl = dbUrl;
        this.dataSource = dataSource;
    }

    public DbUrl getDbUrl() {
        return dbUrl;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @SuppressWarnings("checkstyle:needbraces")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DbDataSource that = (DbDataSource) o;
        return Objects.equals(dbUrl, that.dbUrl) &&
                Objects.equals(dataSource, that.dataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbUrl, dataSource);
    }
}
