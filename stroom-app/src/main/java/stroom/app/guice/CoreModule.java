package stroom.app.guice;

import stroom.security.impl.SessionSecurityModule;
import stroom.util.io.HomeDirProvider;
import stroom.util.io.HomeDirProviderImpl;
import stroom.util.io.TempDirProvider;
import stroom.util.io.TempDirProviderImpl;

import com.google.inject.AbstractModule;

public class CoreModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new stroom.activity.impl.db.ActivityDbModule());
        install(new stroom.annotation.impl.db.AnnotationDbModule());
        install(new stroom.annotation.pipeline.AnnotationPipelineModule());
        install(new stroom.cache.impl.CacheModule());
        install(new stroom.cache.impl.CacheResourceModule());
        install(new stroom.cluster.lock.impl.db.ClusterLockDbModule());
        install(new stroom.cluster.task.impl.ClusterTaskModule());
        install(new stroom.config.global.impl.db.GlobalConfigDbModule());
        install(new stroom.core.dataprocess.PipelineStreamTaskModule());
        install(new stroom.core.db.DbStatusModule());
        install(new stroom.core.entity.event.EntityEventModule());
        install(new stroom.core.query.QueryModule());
        install(new stroom.core.receive.ReceiveDataModule());
        install(new stroom.core.servlet.ServletModule());
        install(new stroom.core.sysinfo.SystemInfoModule());
        install(new stroom.core.welcome.SessionInfoModule());
        install(new stroom.core.welcome.WelcomeModule());
        install(new stroom.dashboard.impl.DashboardModule());
        install(new stroom.dashboard.impl.datasource.DataSourceModule());
        install(new stroom.dashboard.impl.logging.LoggingModule());
        install(new stroom.dashboard.impl.script.ScriptModule());
        install(new stroom.dashboard.impl.visualisation.VisualisationModule());
        install(new stroom.data.retention.impl.DataRetentionModule());
        install(new stroom.data.store.impl.DataStoreModule());
        install(new stroom.data.store.impl.fs.FsDataStoreModule());
        install(new stroom.data.store.impl.fs.FsDataStoreTaskHandlerModule());
        install(new stroom.data.store.impl.fs.db.FsDataStoreDbModule());
        install(new stroom.dictionary.impl.DictionaryHandlerModule());
        install(new stroom.dictionary.impl.DictionaryModule());
        install(new stroom.docstore.impl.DocStoreModule());
        install(new stroom.docstore.impl.db.DBPersistenceModule());
        install(new stroom.event.logging.impl.EventLoggingModule());
        install(new stroom.event.logging.rs.impl.RestResourceAutoLoggerModule());
        install(new stroom.explorer.impl.ExplorerModule());
        install(new stroom.explorer.impl.db.ExplorerDbModule());
        install(new stroom.feed.impl.FeedModule());
        install(new stroom.importexport.impl.ExportConfigResourceModule());
        install(new stroom.importexport.impl.ImportExportHandlerModule());
        install(new stroom.importexport.impl.ImportExportModule());
        install(new stroom.index.impl.IndexElementModule());
        install(new stroom.index.impl.IndexModule());
        install(new stroom.index.impl.db.IndexDbModule());
        install(new stroom.job.impl.JobSystemModule());
        install(new stroom.job.impl.db.JobDbModule());
        install(new stroom.kafka.impl.KafkaConfigHandlerModule());
        install(new stroom.kafka.impl.KafkaConfigModule());
        install(new stroom.kafka.pipeline.KafkaPipelineModule());
        install(new stroom.legacy.db.LegacyDbModule());
        install(new stroom.legacy.impex_6_1.LegacyImpexModule());
        install(new stroom.meta.impl.MetaModule());
        install(new stroom.meta.impl.db.MetaDbModule());
        install(new stroom.node.impl.NodeModule());
        install(new stroom.node.impl.db.NodeDbModule());
        install(new stroom.pipeline.PipelineModule());
        install(new stroom.pipeline.cache.PipelineCacheModule());
        install(new stroom.pipeline.factory.CommonPipelineElementModule());
        install(new stroom.pipeline.factory.DataStorePipelineElementModule());
        install(new stroom.pipeline.factory.PipelineFactoryModule());
        install(new stroom.pipeline.refdata.ReferenceDataModule());
        install(new stroom.pipeline.stepping.PipelineSteppingModule());
        install(new stroom.pipeline.xsltfunctions.CommonXsltFunctionModule());
        install(new stroom.pipeline.xsltfunctions.DataStoreXsltFunctionModule());
        install(new stroom.processor.impl.ProcessorModule());
        install(new stroom.processor.impl.db.ProcessorDbModule());
        install(new stroom.receive.common.RemoteFeedModule());
        install(new stroom.receive.rules.impl.ReceiveDataRuleSetModule());
        install(new stroom.search.extraction.ExtractionModule());
        install(new stroom.search.impl.SearchModule());
        install(new stroom.search.impl.shard.ShardModule());
        install(new stroom.search.elastic.ElasticSearchModule());
        install(new stroom.search.solr.SolrSearchModule());
        install(new stroom.searchable.impl.SearchableModule());
        install(new stroom.security.identity.IdentityModule());
        install(new stroom.security.identity.db.IdentityDbModule());
        install(new stroom.security.impl.SecurityModule());
        install(new stroom.security.impl.db.SecurityDbModule());
        install(new SessionSecurityModule());
        install(new stroom.servicediscovery.impl.ServiceDiscoveryModule());
        install(new stroom.statistics.impl.InternalStatisticsModule());
        install(new stroom.statistics.impl.hbase.entity.StroomStatsStoreModule());
        install(new stroom.statistics.impl.hbase.internal.InternalModule());
        install(new stroom.statistics.impl.hbase.pipeline.StatisticsElementModule());
        install(new stroom.statistics.impl.hbase.rollup.StroomStatsRollupModule());
        install(new stroom.statistics.impl.sql.SQLStatisticsModule());
        install(new stroom.statistics.impl.sql.entity.StatisticStoreModule());
        install(new stroom.statistics.impl.sql.filter.StatisticsElementsModule());
        install(new stroom.statistics.impl.sql.internal.InternalModule());
        install(new stroom.statistics.impl.sql.rollup.SQLStatisticRollupModule());
        install(new stroom.statistics.impl.sql.search.SQLStatisticSearchModule());
        install(new stroom.storedquery.impl.StoredQueryModule());
        install(new stroom.storedquery.impl.db.StoredQueryDbModule());
        install(new stroom.task.impl.TaskModule());
        install(new stroom.util.pipeline.scope.PipelineScopeModule());

        // Bind the directory providers.
        bind(HomeDirProvider.class).to(HomeDirProviderImpl.class);
        bind(TempDirProvider.class).to(TempDirProviderImpl.class);
    }
}
