package stroom.app.guice;

import stroom.app.uri.UriFactoryModule;
import stroom.cluster.impl.ClusterModule;
import stroom.config.app.AppConfig;
import stroom.config.app.AppConfigModule;
import stroom.config.app.Config;
import stroom.config.app.ConfigHolder;
import stroom.db.util.DbModule;
import stroom.dropwizard.common.LogLevelInspector;
import stroom.index.impl.IndexShardWriterExecutorProvider;
import stroom.index.impl.IndexShardWriterExecutorProviderImpl;
import stroom.lifecycle.impl.LifecycleServiceModule;
import stroom.meta.statistics.impl.MetaStatisticsModule;
import stroom.resource.impl.SessionResourceModule;
import stroom.security.impl.SecurityContextModule;
import stroom.statistics.impl.sql.search.SQLStatisticSearchModule;
import stroom.util.guice.HasSystemInfoBinder;

import com.google.inject.AbstractModule;
import io.dropwizard.setup.Environment;

import java.nio.file.Path;

public class AppModule extends AbstractModule {

    private final Config configuration;
    private final Environment environment;
    private final ConfigHolder configHolder;

    public AppModule(final Config configuration,
                     final Environment environment,
                     final Path configFile) {
        this.configuration = configuration;
        this.environment = environment;

        configHolder = new ConfigHolder() {
            @Override
            public AppConfig getAppConfig() {
                return configuration.getAppConfig();
            }

            @Override
            public Path getConfigFile() {
                return configFile;
            }
        };
    }

    /**
     * Alternative constructor for when we are running the app in the absence of
     * the DW Environment and jetty server, i.e. for DB migrations.
     */
    public AppModule(final Config configuration,
                     final Path configFile) {
        this(configuration, null, configFile);
    }

    @Override
    protected void configure() {
        bind(Config.class).toInstance(configuration);

        // Allows us to load up the app in the absence of a the DW jersey environment
        // e.g. for migrations
        if (environment != null) {
            bind(Environment.class).toInstance(environment);
        }

        install(new AppConfigModule(configHolder));
        install(new UriFactoryModule());
        install(new DbModule());
        install(new CoreModule());
        install(new LifecycleServiceModule());
        install(new JobsModule());
        install(new ClusterModule());
        install(new SecurityContextModule());
        install(new MetaStatisticsModule());
        install(new SQLStatisticSearchModule());
        install(new SessionResourceModule());
        install(new JerseyModule());
        bind(IndexShardWriterExecutorProvider.class).to(IndexShardWriterExecutorProviderImpl.class);

        HasSystemInfoBinder.create(binder())
                .bind(LogLevelInspector.class);
    }

}
