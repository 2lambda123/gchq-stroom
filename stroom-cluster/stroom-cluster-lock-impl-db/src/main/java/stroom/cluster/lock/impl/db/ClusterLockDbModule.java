package stroom.cluster.lock.impl.db;

import stroom.cluster.lock.api.ClusterLockService;
import stroom.db.util.AbstractFlyWayDbModule;
import stroom.db.util.DataSourceProxy;
import stroom.job.api.ScheduledJobsBinder;
import stroom.util.RunnableWrapper;
import stroom.util.guice.GuiceUtil;
import stroom.util.guice.RestResourcesBinder;
import stroom.util.shared.Clearable;

import javax.inject.Inject;
import javax.sql.DataSource;

import static stroom.job.api.Schedule.ScheduleType.PERIODIC;

public class ClusterLockDbModule extends AbstractFlyWayDbModule<ClusterLockConfig, ClusterLockDbConnProvider> {

    private static final String MODULE = "stroom-cluster-lock";
    private static final String FLYWAY_LOCATIONS = "stroom/cluster/lock/impl/db/migration";
    private static final String FLYWAY_TABLE = "cluster_lock_schema_history";

    @Override
    protected void configure() {
        super.configure();
        bind(ClusterLockService.class).to(ClusterLockServiceImpl.class);
        bind(ClusterLockResource.class).to(ClusterLockResourceImpl.class);

        GuiceUtil.buildMultiBinder(binder(), Clearable.class)
                .addBinding(DbClusterLock.class);

        RestResourcesBinder.create(binder())
                .bind(ClusterLockResourceImpl.class);

        ScheduledJobsBinder.create(binder())
                .bindJobTo(UnlockOldLocks.class, builder -> builder
                        .name("Unlock old locks")
                        .description("Every 10 minutes try and unlock/remove any locks that " +
                                "we hold that have not been refreshed by their owner for 10 minutes.")
                        .managed(false)
                        .schedule(PERIODIC, "10m"))
                .bindJobTo(KeepAlive.class, builder -> builder
                        .name("Keep alive")
                        .description("Keeps a locks alive")
                        .managed(false)
                        .schedule(PERIODIC, "1m"));
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
    protected Class<ClusterLockDbConnProvider> getConnectionProviderType() {
        return ClusterLockDbConnProvider.class;
    }

    @Override
    protected ClusterLockDbConnProvider createConnectionProvider(final DataSource dataSource) {
        return new DataSourceImpl(dataSource);
    }

    private static class DataSourceImpl extends DataSourceProxy implements ClusterLockDbConnProvider {

        private DataSourceImpl(final DataSource dataSource) {
            super(dataSource);
        }
    }

    private static class UnlockOldLocks extends RunnableWrapper {

        @Inject
        UnlockOldLocks(final ClusterLockClusterHandler clusterLockClusterHandler) {
            super(clusterLockClusterHandler::unlockOldLocks);
        }
    }

    private static class KeepAlive extends RunnableWrapper {

        @Inject
        KeepAlive(final ClusterLockServiceImpl clusterLockService) {
            super(clusterLockService::keepAlive);
        }
    }
}
