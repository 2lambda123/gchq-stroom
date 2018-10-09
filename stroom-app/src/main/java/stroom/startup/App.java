/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.startup;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.servlets.tasks.LogConfigurationTask;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import stroom.cluster.server.ClusterCallServiceRPC;
import stroom.content.ContentSyncService;
import stroom.content.ProxySecurityFilter;
import stroom.dashboard.spring.DashboardConfiguration;
import stroom.datafeed.server.DataFeedServlet;
import stroom.dictionary.server.DictionaryResource;
import stroom.dictionary.server.DictionaryStore;
import stroom.dictionary.shared.DictionaryDoc;
import stroom.dictionary.spring.DictionaryConfiguration;
import stroom.dispatch.shared.DispatchService;
import stroom.elastic.spring.ElasticIndexConfiguration;
import stroom.entity.server.SpringRequestFactoryServlet;
import stroom.entity.server.util.ConnectionUtil;
import stroom.explorer.server.ExplorerConfiguration;
import stroom.externaldoc.spring.ExternalDocRefConfiguration;
import stroom.feed.server.RemoteFeedServiceRPC;
import stroom.importexport.server.ImportExportActionHandler;
import stroom.index.server.StroomIndexQueryResource;
import stroom.index.spring.IndexConfiguration;
import stroom.lifecycle.LifecycleService;
import stroom.logging.spring.EventLoggingConfiguration;
import stroom.pipeline.spring.PipelineConfiguration;
import stroom.proxy.guice.ProxyModule;
import stroom.proxy.repo.ProxyLifecycle;
import stroom.proxy.servlet.ConfigServlet;
import stroom.proxy.servlet.ProxyStatusServlet;
import stroom.proxy.servlet.ProxyWelcomeServlet;
import stroom.ruleset.server.RuleSetResource;
import stroom.ruleset.server.RuleSetService;
import stroom.ruleset.shared.RuleSet;
import stroom.ruleset.spring.RuleSetConfiguration;
import stroom.script.server.ScriptServlet;
import stroom.script.spring.ScriptConfiguration;
import stroom.search.spring.SearchConfiguration;
import stroom.security.server.AuthorisationResource;
import stroom.security.server.SecurityFilter;
import stroom.security.server.SessionResource;
import stroom.security.spring.SecurityConfiguration;
import stroom.servicediscovery.ResourcePaths;
import stroom.servicediscovery.ServiceDiscovererImpl;
import stroom.servicediscovery.ServiceDiscoveryRegistrar;
import stroom.servlet.CacheControlFilter;
import stroom.servlet.DashboardServlet;
import stroom.servlet.DebugServlet;
import stroom.servlet.DynamicCSSServlet;
import stroom.servlet.EchoServlet;
import stroom.servlet.ExportConfigServlet;
import stroom.servlet.HttpServletRequestFilter;
import stroom.servlet.ImportFileServlet;
import stroom.servlet.RejectPostFilter;
import stroom.servlet.SessionListListener;
import stroom.servlet.SessionListServlet;
import stroom.servlet.SessionResourceStoreImpl;
import stroom.servlet.StatusServlet;
import stroom.servlet.StroomServlet;
import stroom.spring.MetaDataStatisticConfiguration;
import stroom.spring.PersistenceConfiguration;
import stroom.spring.ScopeConfiguration;
import stroom.spring.ServerComponentScanConfiguration;
import stroom.spring.ServerConfiguration;
import stroom.statistics.server.sql.search.SqlStatisticsQueryResource;
import stroom.statistics.spring.StatisticsConfiguration;
import stroom.util.config.StroomProperties;
import stroom.util.db.DbUtil;
import stroom.util.spring.StroomSpringProfiles;
import stroom.visualisation.spring.VisualisationConfiguration;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class App extends Application<Config> {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static String configPath;

    public static void main(final String[] args) throws Exception {
        if (args.length > 0) {
            configPath = args[args.length - 1];
        }

        // Hibernate requires JBoss Logging. The SLF4J API jar wasn't being detected so this sets it manually.
        System.setProperty("org.jboss.logging.provider", "slf4j");
        new App().run(args);
    }

    @Override
    public void initialize(final Bootstrap<Config> bootstrap) {
        // This allows us to use templating in the YAML configuration.
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)));
        bootstrap.addBundle(new AssetsBundle("/ui", ResourcePaths.ROOT_PATH, "index.html", "ui"));
    }

    @Override
    public void run(final Config configuration, final Environment environment) {
        // Add useful logging setup.
        registerLogConfiguration(environment);
        environment.healthChecks().register(LogLevelInspector.class.getName(), new LogLevelInspector());

        // We want Stroom to use the root path so we need to move Dropwizard's path.
        environment.jersey().setUrlPattern(ResourcePaths.API_PATH + "/*");

        // Set up a session manager for Jetty
        SessionHandler sessions = new SessionHandler();
        environment.servlets().setSessionHandler(sessions);

        // Configure Cross-Origin Resource Sharing.
        configureCors(environment);

        if ("proxy".equalsIgnoreCase(configuration.getMode())) {
            startProxy(configuration, environment);
        } else {
//            // Adding asset bundles this way is not normal but it is done so that proxy can serve it's own root page for now.
//            new AssetsBundle("/ui", "/", "stroom", "ui").run(environment);
            startApp(configuration, environment);
        }
    }

    private void startProxy(final Config configuration, final Environment environment) {
        final ProxyModule proxyModule = new ProxyModule(configuration.getProxyConfig());
        final Injector injector = Guice.createInjector(proxyModule);

        final ServletContextHandler servletContextHandler = environment.getApplicationContext();

        // Add health checks
        GuiceUtil.addHealthCheck(environment.healthChecks(), injector, DictionaryResource.class);
        GuiceUtil.addHealthCheck(environment.healthChecks(), injector, RuleSetResource.class);

        // Add filters
        GuiceUtil.addFilter(servletContextHandler, injector, ProxySecurityFilter.class, "/*");

        // Add servlets
        servletContextHandler.addServlet(new ServletHolder(new ConfigServlet(configPath)), ResourcePaths.ROOT_PATH + "/config");
        GuiceUtil.addServlet(servletContextHandler, injector, DataFeedServlet.class, ResourcePaths.ROOT_PATH + "/datafeed");
        GuiceUtil.addServlet(servletContextHandler, injector, DataFeedServlet.class, ResourcePaths.ROOT_PATH + "/datafeed/*");
        GuiceUtil.addServlet(servletContextHandler, injector, ProxyWelcomeServlet.class, ResourcePaths.ROOT_PATH + "/ui");
        GuiceUtil.addServlet(servletContextHandler, injector, ProxyStatusServlet.class, ResourcePaths.ROOT_PATH + "/status");
        GuiceUtil.addServlet(servletContextHandler, injector, DebugServlet.class, ResourcePaths.ROOT_PATH + "/debug");

        // Add resources.
        GuiceUtil.addResource(environment.jersey(), injector, DictionaryResource.class);
        GuiceUtil.addResource(environment.jersey(), injector, RuleSetResource.class);

        // Listen to the lifecycle of the Dropwizard app.
        GuiceUtil.manage(environment.lifecycle(), injector, ProxyLifecycle.class);

        // Sync content.
        if (configuration.getProxyConfig() != null && configuration.getProxyConfig().getContentSyncConfig() != null) {
            // Create a map of import handlers.
            final Map<String, ImportExportActionHandler> importExportActionHandlers = new HashMap<>();
            importExportActionHandlers.put(RuleSet.DOCUMENT_TYPE, injector.getInstance(RuleSetService.class));
            importExportActionHandlers.put(DictionaryDoc.ENTITY_TYPE, injector.getInstance(DictionaryStore.class));

            final ContentSyncService contentSyncService = new ContentSyncService(configuration.getProxyConfig().getContentSyncConfig(), importExportActionHandlers);
            environment.lifecycle().manage(contentSyncService);
        }
    }

    private void startApp(final Config configuration, final Environment environment) {

        // Make sure we can connect to our databases, retrying as required.
        boolean didAllDbsConnect;
        didAllDbsConnect = waitForStroomDbConnection();
        didAllDbsConnect = waitForStatsDbConnection() && didAllDbsConnect;
        if (!didAllDbsConnect) {
            LOGGER.error("Can't connect to all databases, shutting down");
            System.exit(1);
        }

        // Start the spring context.
        LOGGER.info("Loading Spring context");
        final ApplicationContext applicationContext = loadApplcationContext(configuration, environment);

        final ServletContextHandler servletContextHandler = environment.getApplicationContext();

        // Add health checks
        SpringUtil.addHealthCheck(environment.healthChecks(), applicationContext, ServiceDiscoveryRegistrar.class);
        SpringUtil.addHealthCheck(environment.healthChecks(), applicationContext, ServiceDiscovererImpl.class);
        SpringUtil.addHealthCheck(environment.healthChecks(), applicationContext, SqlStatisticsQueryResource.class);
        SpringUtil.addHealthCheck(environment.healthChecks(), applicationContext, StroomIndexQueryResource.class);
        SpringUtil.addHealthCheck(environment.healthChecks(), applicationContext, DictionaryResource.class);
        SpringUtil.addHealthCheck(environment.healthChecks(), applicationContext, RuleSetResource.class);

        // Add filters
        SpringUtil.addFilter(servletContextHandler, applicationContext, HttpServletRequestFilter.class, "/*");
        FilterUtil.addFilter(servletContextHandler, RejectPostFilter.class, "rejectPostFilter").setInitParameter("rejectUri", "/");
        FilterUtil.addFilter(servletContextHandler, CacheControlFilter.class, "cacheControlFilter").setInitParameter("seconds", "600");
        SpringUtil.addFilter(servletContextHandler, applicationContext, SecurityFilter.class, "/*");

        // Add servlets
        SpringUtil.addServlet(servletContextHandler, applicationContext, StroomServlet.class, ResourcePaths.ROOT_PATH + "/ui");
        SpringUtil.addServlet(servletContextHandler, applicationContext, DashboardServlet.class, ResourcePaths.ROOT_PATH + "/dashboard");
        SpringUtil.addServlet(servletContextHandler, applicationContext, DynamicCSSServlet.class, ResourcePaths.ROOT_PATH + "/dynamic.css");
        SpringUtil.addServlet(servletContextHandler, applicationContext, DispatchService.class, ResourcePaths.ROOT_PATH + "/dispatch.rpc");
        SpringUtil.addServlet(servletContextHandler, applicationContext, ImportFileServlet.class, ResourcePaths.ROOT_PATH + "/importfile.rpc");
        SpringUtil.addServlet(servletContextHandler, applicationContext, ScriptServlet.class, ResourcePaths.ROOT_PATH + "/script");
        SpringUtil.addServlet(servletContextHandler, applicationContext, ClusterCallServiceRPC.class, ResourcePaths.ROOT_PATH + "/clustercall.rpc");
        SpringUtil.addServlet(servletContextHandler, applicationContext, ExportConfigServlet.class, ResourcePaths.ROOT_PATH + "/export");
        SpringUtil.addServlet(servletContextHandler, applicationContext, StatusServlet.class, ResourcePaths.ROOT_PATH + "/status");
        SpringUtil.addServlet(servletContextHandler, applicationContext, EchoServlet.class, ResourcePaths.ROOT_PATH + "/echo");
        SpringUtil.addServlet(servletContextHandler, applicationContext, DebugServlet.class, ResourcePaths.ROOT_PATH + "/debug");
        SpringUtil.addServlet(servletContextHandler, applicationContext, SessionListServlet.class, ResourcePaths.ROOT_PATH + "/sessionList");
        SpringUtil.addServlet(servletContextHandler, applicationContext, SessionResourceStoreImpl.class, ResourcePaths.ROOT_PATH + "/resourcestore/*");
        SpringUtil.addServlet(servletContextHandler, applicationContext, SpringRequestFactoryServlet.class, ResourcePaths.ROOT_PATH + "/gwtRequest");
        SpringUtil.addServlet(servletContextHandler, applicationContext, RemoteFeedServiceRPC.class, ResourcePaths.ROOT_PATH + "/remoting/remotefeedservice.rpc");
        SpringUtil.addServlet(servletContextHandler, applicationContext, DataFeedServlet.class, ResourcePaths.ROOT_PATH + "/datafeed");
        SpringUtil.addServlet(servletContextHandler, applicationContext, DataFeedServlet.class, ResourcePaths.ROOT_PATH + "/datafeed/*");

        // Add session listeners.
        SpringUtil.addServletListener(environment.servlets(), applicationContext, SessionListListener.class);

        // Add resources.
        SpringUtil.addResource(environment.jersey(), applicationContext, DictionaryResource.class);
        SpringUtil.addResource(environment.jersey(), applicationContext, RuleSetResource.class);
        SpringUtil.addResource(environment.jersey(), applicationContext, StroomIndexQueryResource.class);
        SpringUtil.addResource(environment.jersey(), applicationContext, SqlStatisticsQueryResource.class);
        SpringUtil.addResource(environment.jersey(), applicationContext, AuthorisationResource.class);
        SpringUtil.addResource(environment.jersey(), applicationContext, SessionResource.class);

        // Listen to the lifecycle of the Dropwizard app.
        SpringUtil.manage(environment.lifecycle(), applicationContext, LifecycleService.class);
    }

    private boolean waitForStroomDbConnection() {
        final String driverClassname = StroomProperties.getProperty(ConnectionUtil.JDBC_DRIVER_CLASS_NAME);
        final String driverUrl = StroomProperties.getProperty(ConnectionUtil.JDBC_DRIVER_URL);
        final String driverUsername = StroomProperties.getProperty(ConnectionUtil.JDBC_DRIVER_USERNAME);
        final String driverPassword = StroomProperties.getProperty(ConnectionUtil.JDBC_DRIVER_PASSWORD);

        return DbUtil.waitForConnection(driverClassname, driverUrl, driverUsername, driverPassword);
    }

    private boolean waitForStatsDbConnection() {
        final String driverClassname = StroomProperties.getProperty("stroom.statistics.sql.jdbcDriverClassName");
        final String driverUrl = StroomProperties.getProperty("stroom.statistics.sql.jdbcDriverUrl|trace");
        final String driverUsername = StroomProperties.getProperty("stroom.statistics.sql.jdbcDriverUsername");
        final String driverPassword = StroomProperties.getProperty("stroom.statistics.sql.jdbcDriverPassword");

        return DbUtil.waitForConnection(driverClassname, driverUrl, driverUsername, driverPassword);
    }

    private ApplicationContext loadApplcationContext(final Configuration configuration, final Environment environment) {
        final AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.getEnvironment().setActiveProfiles(StroomSpringProfiles.PROD, SecurityConfiguration.PROD_SECURITY);
        applicationContext.getBeanFactory().registerSingleton("dwConfiguration", configuration);
        applicationContext.getBeanFactory().registerSingleton("dwEnvironment", environment);
        applicationContext.register(
                ScopeConfiguration.class,
                PersistenceConfiguration.class,
                ServerComponentScanConfiguration.class,
                ServerConfiguration.class,
                EventLoggingConfiguration.class,
                DictionaryConfiguration.class,
                PipelineConfiguration.class,
                ExplorerConfiguration.class,
                IndexConfiguration.class,
                SearchConfiguration.class,
                ScriptConfiguration.class,
                VisualisationConfiguration.class,
                DashboardConfiguration.class,
                MetaDataStatisticConfiguration.class,
                StatisticsConfiguration.class,
                SecurityConfiguration.class,
                ExternalDocRefConfiguration.class,
                ElasticIndexConfiguration.class,
                RuleSetConfiguration.class
        );
        applicationContext.refresh();
        return applicationContext;
    }

    private static void configureCors(io.dropwizard.setup.Environment environment) {
        FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "*");
    }

    private void registerLogConfiguration(final Environment environment) {
        // Task to allow configuration of log levels at runtime
        String path = environment.getAdminContext().getContextPath();

        // To change the log level do one of:
        // curl -X POST -d "logger=stroom&level=DEBUG" [admin context path]/tasks/log-level
        // http -f POST [admin context path]/tasks/log-level logger=stroom level=DEBUG
        // 'http' requires installing HTTPie

        LOGGER.info("Registering Log Configuration Task on {}/tasks/log-level", path);
        environment.admin().addTask(new LogConfigurationTask());
    }
}
