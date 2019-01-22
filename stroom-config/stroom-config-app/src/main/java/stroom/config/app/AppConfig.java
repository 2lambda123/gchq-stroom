package stroom.config.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import stroom.activity.impl.db.ActivityConfig;
import stroom.benchmark.BenchmarkClusterConfig;
import stroom.cluster.ClusterConfig;
import stroom.dashboard.QueryHistoryConfig;
import stroom.datafeed.DataFeedConfig;
import stroom.datasource.DataSourceUrlConfig;
import stroom.explorer.impl.db.ExplorerConfig;
import stroom.importexport.ContentPackImportConfig;
import stroom.index.IndexConfig;
import stroom.job.JobSystemConfig;
import stroom.lifecycle.impl.LifecycleConfig;
import stroom.node.NodeConfig;
import stroom.persist.CoreConfig;
import stroom.pipeline.PipelineConfig;
import stroom.policy.PolicyConfig;
import stroom.refdata.store.RefDataStoreConfig;
import stroom.search.SearchConfig;
import stroom.security.SecurityConfig;
import stroom.security.impl.db.SecurityDbConfig;
import stroom.servicediscovery.ServiceDiscoveryConfig;
import stroom.servlet.ExportConfig;
import stroom.statistics.StatisticsConfig;
import stroom.streamtask.ProcessConfig;
import stroom.streamtask.ProxyAggregationConfig;
import stroom.ui.config.shared.UiConfig;
import stroom.util.shared.IsConfig;
import stroom.volume.VolumeConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AppConfig implements IsConfig {
    private ActivityConfig activityConfig;
    private BenchmarkClusterConfig benchmarkClusterConfig;
    private ClusterConfig clusterConfig;
    private ContentPackImportConfig contentPackImportConfig;
    private CoreConfig coreConfig;
    private DataConfig dataConfig;
    private DataFeedConfig dataFeedConfig;
    private DataSourceUrlConfig dataSourceUrlConfig;
    private ExplorerConfig explorerConfig;
    private ExportConfig exportConfig;
    private IndexConfig indexConfig;
    private JobSystemConfig jobSystemConfig;
    private LifecycleConfig lifecycleConfig;
    private NodeConfig nodeConfig;
    private PipelineConfig pipelineConfig;
    private PolicyConfig policyConfig;
    private ProcessConfig processConfig;
    private PropertyServiceConfig propertyServiceConfig;
    private ProxyAggregationConfig proxyAggregationConfig;
    private QueryHistoryConfig queryHistoryConfig;
    private RefDataStoreConfig refDataStoreConfig;
    private SearchConfig searchConfig;
    private SecurityConfig securityConfig;
    private SecurityDbConfig securityDbConfig;
    private ServiceDiscoveryConfig serviceDiscoveryConfig;
    private StatisticsConfig statisticsConfig;
    private UiConfig uiConfig;
    private VolumeConfig volumeConfig;

    public AppConfig() {
        this.activityConfig = new ActivityConfig();
        this.benchmarkClusterConfig = new BenchmarkClusterConfig();
        this.clusterConfig = new ClusterConfig();
        this.contentPackImportConfig = new ContentPackImportConfig();
        this.coreConfig = new CoreConfig();
        this.dataConfig = new DataConfig();
        this.dataFeedConfig = new DataFeedConfig();
        this.dataSourceUrlConfig = new DataSourceUrlConfig();
        this.explorerConfig = new ExplorerConfig();
        this.exportConfig = new ExportConfig();
        this.indexConfig = new IndexConfig();
        this.jobSystemConfig = new JobSystemConfig();
        this.lifecycleConfig = new LifecycleConfig();
        this.pipelineConfig = new PipelineConfig();
        this.nodeConfig = new NodeConfig();
        this.policyConfig = new PolicyConfig();
        this.processConfig = new ProcessConfig();
        this.propertyServiceConfig = new PropertyServiceConfig();
        this.proxyAggregationConfig = new ProxyAggregationConfig();
        this.queryHistoryConfig = new QueryHistoryConfig();
        this.refDataStoreConfig = new RefDataStoreConfig();
        this.searchConfig = new SearchConfig();
        this.securityConfig = new SecurityConfig();
        this.securityDbConfig = new SecurityDbConfig();
        this.serviceDiscoveryConfig = new ServiceDiscoveryConfig();
        this.statisticsConfig = new StatisticsConfig();
        this.uiConfig = new UiConfig();
        this.volumeConfig = new VolumeConfig();
    }

    @Inject
    AppConfig(final ActivityConfig activityConfig,
              final BenchmarkClusterConfig benchmarkClusterConfig,
              final ClusterConfig clusterConfig,
              final ContentPackImportConfig contentPackImportConfig,
              final CoreConfig coreConfig,
              final DataConfig dataConfig,
              final DataFeedConfig dataFeedConfig,
              final DataSourceUrlConfig dataSourceUrlConfig,
              final ExplorerConfig explorerConfig,
              final ExportConfig exportConfig,
              final IndexConfig indexConfig,
              final JobSystemConfig jobSystemConfig,
              final LifecycleConfig lifecycleConfig,
              final PipelineConfig pipelineConfig,
              final NodeConfig nodeConfig,
              final PolicyConfig policyConfig,
              final ProcessConfig processConfig,
              final PropertyServiceConfig propertyServiceConfig,
              final ProxyAggregationConfig proxyAggregationConfig,
              final QueryHistoryConfig queryHistoryConfig,
              final RefDataStoreConfig refDataStoreConfig,
              final SearchConfig searchConfig,
              final SecurityConfig securityConfig,
              final SecurityDbConfig securityDbConfig,
              final ServiceDiscoveryConfig serviceDiscoveryConfig,
              final StatisticsConfig statisticsConfig,
              final UiConfig uiConfig,
              final VolumeConfig volumeConfig) {
        this.activityConfig = activityConfig;
        this.benchmarkClusterConfig = benchmarkClusterConfig;
        this.clusterConfig = clusterConfig;
        this.contentPackImportConfig = contentPackImportConfig;
        this.coreConfig = coreConfig;
        this.dataConfig = dataConfig;
        this.dataFeedConfig = dataFeedConfig;
        this.dataSourceUrlConfig = dataSourceUrlConfig;
        this.explorerConfig = explorerConfig;
        this.exportConfig = exportConfig;
        this.indexConfig = indexConfig;
        this.jobSystemConfig = jobSystemConfig;
        this.lifecycleConfig = lifecycleConfig;
        this.pipelineConfig = pipelineConfig;
        this.nodeConfig = nodeConfig;
        this.policyConfig = policyConfig;
        this.processConfig = processConfig;
        this.propertyServiceConfig = propertyServiceConfig;
        this.proxyAggregationConfig = proxyAggregationConfig;
        this.queryHistoryConfig = queryHistoryConfig;
        this.refDataStoreConfig = refDataStoreConfig;
        this.searchConfig = searchConfig;
        this.securityConfig = securityConfig;
        this.securityDbConfig = securityDbConfig;
        this.serviceDiscoveryConfig = serviceDiscoveryConfig;
        this.statisticsConfig = statisticsConfig;
        this.uiConfig = uiConfig;
        this.volumeConfig = volumeConfig;
    }

    @JsonProperty("activity")
    public ActivityConfig getActivityConfig() {
        return activityConfig;
    }

    public void setActivityConfig(final ActivityConfig activityConfig) {
        this.activityConfig = activityConfig;
    }

    @JsonProperty("benchmark")
    public BenchmarkClusterConfig getBenchmarkClusterConfig() {
        return benchmarkClusterConfig;
    }

    public void setBenchmarkClusterConfig(final BenchmarkClusterConfig benchmarkClusterConfig) {
        this.benchmarkClusterConfig = benchmarkClusterConfig;
    }

    @JsonProperty("cluster")
    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    public void setClusterConfig(final ClusterConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

    @JsonProperty("contentPackImport")
    public ContentPackImportConfig getContentPackImportConfig() {
        return contentPackImportConfig;
    }

    public void setContentPackImportConfig(final ContentPackImportConfig contentPackImportConfig) {
        this.contentPackImportConfig = contentPackImportConfig;
    }

    @JsonProperty("core")
    @JsonPropertyDescription("Configuration for the core stroom DB")
    public CoreConfig getCoreConfig() {
        return coreConfig;
    }

    public void setCoreConfig(final CoreConfig coreConfig) {
        this.coreConfig = coreConfig;
    }

    @JsonProperty("data")
    @JsonPropertyDescription("Configuration for the data layer of stroom")
    public DataConfig getDataConfig() {
        return dataConfig;
    }

    public void setDataConfig(final DataConfig dataConfig) {
        this.dataConfig = dataConfig;
    }

    @JsonProperty("feed")
    public DataFeedConfig getDataFeedConfig() {
        return dataFeedConfig;
    }

    public void setDataFeedConfig(final DataFeedConfig dataFeedConfig) {
        this.dataFeedConfig = dataFeedConfig;
    }

    @JsonProperty("dataSourceUrl")
    public DataSourceUrlConfig getDataSourceUrlConfig() {
        return dataSourceUrlConfig;
    }

    public void setDataSourceUrlConfig(final DataSourceUrlConfig dataSourceUrlConfig) {
        this.dataSourceUrlConfig = dataSourceUrlConfig;
    }

    @JsonProperty("explorer")
    public ExplorerConfig getExplorerConfig() {
        return explorerConfig;
    }

    public void setExplorerConfig(final ExplorerConfig explorerConfig) {
        this.explorerConfig = explorerConfig;
    }

    @JsonProperty("export")
    public ExportConfig getExportConfig() {
        return exportConfig;
    }

    public void setExportConfig(final ExportConfig exportConfig) {
        this.exportConfig = exportConfig;
    }

    @JsonProperty("index")
    public IndexConfig getIndexConfig() {
        return indexConfig;
    }

    public void setIndexConfig(final IndexConfig indexConfig) {
        this.indexConfig = indexConfig;
    }

    @JsonProperty("job")
    public JobSystemConfig getJobSystemConfig() {
        return jobSystemConfig;
    }

    public void setJobSystemConfig(final JobSystemConfig jobSystemConfig) {
        this.jobSystemConfig = jobSystemConfig;
    }

    @JsonProperty("lifecycle")
    public LifecycleConfig getLifecycleConfig() {
        return lifecycleConfig;
    }

    public void setLifecycleConfig(final LifecycleConfig lifecycleConfig) {
        this.lifecycleConfig = lifecycleConfig;
    }

    @JsonProperty("node")
    public NodeConfig getNodeConfig() {
        return nodeConfig;
    }

    public void setNodeConfig(final NodeConfig nodeConfig) {
        this.nodeConfig = nodeConfig;
    }

    @JsonProperty("pipeline")
    public PipelineConfig getPipelineConfig() {
        return pipelineConfig;
    }

    public void setPipelineConfig(final PipelineConfig pipelineConfig) {
        this.pipelineConfig = pipelineConfig;
    }

    @JsonProperty("policy")
    public PolicyConfig getPolicyConfig() {
        return policyConfig;
    }

    public void setPolicyConfig(final PolicyConfig policyConfig) {
        this.policyConfig = policyConfig;
    }

    @JsonProperty("process")
    public ProcessConfig getProcessConfig() {
        return processConfig;
    }

    public void setProcessConfig(final ProcessConfig processConfig) {
        this.processConfig = processConfig;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Configuration for the stroom property service")
    public PropertyServiceConfig getPropertyServiceConfig() {
        return propertyServiceConfig;
    }

    public void setPropertyServiceConfig(final PropertyServiceConfig propertyServiceConfig) {
        this.propertyServiceConfig = propertyServiceConfig;
    }

    @JsonProperty("proxyAggregation")
    public ProxyAggregationConfig getProxyAggregationConfig() {
        return proxyAggregationConfig;
    }

    public void setProxyAggregationConfig(final ProxyAggregationConfig proxyAggregationConfig) {
        this.proxyAggregationConfig = proxyAggregationConfig;
    }

    @JsonProperty("queryHistory")
    public QueryHistoryConfig getQueryHistoryConfig() {
        return queryHistoryConfig;
    }

    public void setQueryHistoryConfig(final QueryHistoryConfig queryHistoryConfig) {
        this.queryHistoryConfig = queryHistoryConfig;
    }

    @JsonProperty("refdata")
    public RefDataStoreConfig getRefDataStoreConfig() {
        return refDataStoreConfig;
    }

    public void setRefDataStoreConfig(final RefDataStoreConfig refDataStoreConfig) {
        this.refDataStoreConfig = refDataStoreConfig;
    }

    @JsonProperty("search")
    public SearchConfig getSearchConfig() {
        return searchConfig;
    }

    public void setSearchConfig(final SearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    @JsonProperty("security")
    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(final SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @JsonProperty("securityDb")
    public SecurityDbConfig getSecurityDbConfig() {
        return securityDbConfig;
    }

    public void setSecurityDbConfig(final SecurityDbConfig securityDbConfig) {
        this.securityDbConfig = securityDbConfig;
    }

    @JsonProperty("serviceDiscovery")
    public ServiceDiscoveryConfig getServiceDiscoveryConfig() {
        return serviceDiscoveryConfig;
    }

    public void setServiceDiscoveryConfig(final ServiceDiscoveryConfig serviceDiscoveryConfig) {
        this.serviceDiscoveryConfig = serviceDiscoveryConfig;
    }

    @JsonProperty("statistics")
    @JsonPropertyDescription("Configuration for the stroom statistics service")
    public StatisticsConfig getStatisticsConfig() {
        return statisticsConfig;
    }

    public void setStatisticsConfig(final StatisticsConfig statisticsConfig) {
        this.statisticsConfig = statisticsConfig;
    }

    @JsonProperty("ui")
    public UiConfig getUiConfig() {
        return uiConfig;
    }

    public void setUiConfig(final UiConfig uiConfig) {
        this.uiConfig = uiConfig;
    }

    @JsonProperty("volumes")
    public VolumeConfig getVolumeConfig() {
        return volumeConfig;
    }

    public void setVolumeConfig(final VolumeConfig volumeConfig) {
        this.volumeConfig = volumeConfig;
    }

}
