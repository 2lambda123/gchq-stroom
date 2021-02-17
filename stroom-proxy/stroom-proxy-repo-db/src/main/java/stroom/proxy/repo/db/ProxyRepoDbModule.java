package stroom.proxy.repo.db;

import stroom.db.util.AbstractFlyWayDbModule;
import stroom.db.util.DataSourceProxy;
import stroom.proxy.repo.ProxyRepositoryConfig;
import stroom.proxy.repo.ZipInfoStore;

import javax.sql.DataSource;

public class ProxyRepoDbModule extends AbstractFlyWayDbModule<ProxyRepositoryConfig, ProxyRepoDbConnProvider> {

    private static final String MODULE = "stroom-proxy-repo";
    private static final String FLYWAY_LOCATIONS = "stroom/proxy/repo/db";
    private static final String FLYWAY_TABLE = "proxy_repo_schema_history";

    @Override
    protected void configure() {
        super.configure();
        bind(ZipInfoStoreDao.class).to(ZipInfoStoreDaoImpl.class);
        bind(ZipInfoStore.class).to(ZipInfoStoreImpl.class);
    }

    @Override
    protected String getFlyWayTableName() {
        return FLYWAY_TABLE;
    }

    @Override
    protected String getModuleName() {
        return MODULE;
    }

    @Override
    protected String getFlyWayLocation() {
        return FLYWAY_LOCATIONS;
    }

    @Override
    protected Class<ProxyRepoDbConnProvider> getConnectionProviderType() {
        return ProxyRepoDbConnProvider.class;
    }

    @Override
    protected ProxyRepoDbConnProvider createConnectionProvider(final DataSource dataSource) {
        return new DataSourceImpl(dataSource);
    }

    private static class DataSourceImpl extends DataSourceProxy implements ProxyRepoDbConnProvider {

        private DataSourceImpl(final DataSource dataSource) {
            super(dataSource);
        }
    }
}
