package stroom.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.docref.DocRef;
import stroom.lifecycle.StroomBeanStore;
import stroom.security.SecurityContext;
import stroom.servicediscovery.ServiceDiscoverer;
import stroom.servicediscovery.ServiceDiscoveryConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
@SuppressWarnings("unused")
public class DataSourceProviderRegistryImpl implements DataSourceProviderRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceProviderRegistryImpl.class);

    private final DataSourceProviderRegistry delegateDataSourceProviderRegistry;

    @SuppressWarnings("unused")
    @Inject
    DataSourceProviderRegistryImpl(final SecurityContext securityContext,
                                   final ServiceDiscoveryConfig serviceDiscoveryConfig,
                                   final StroomBeanStore stroomBeanStore,
                                   final DataSourceUrlConfig dataSourceUrlConfig) {
        final boolean isServiceDiscoveryEnabled = serviceDiscoveryConfig.isEnabled();
        if (isServiceDiscoveryEnabled) {
            ServiceDiscoverer serviceDiscoverer = stroomBeanStore.getInstance(ServiceDiscoverer.class);
            LOGGER.debug("Using service discovery for service lookup");
            delegateDataSourceProviderRegistry = new ServiceDiscoveryDataSourceProviderRegistry(
                    securityContext,
                    serviceDiscoverer);
        } else {
            LOGGER.debug("Using local services");
            delegateDataSourceProviderRegistry = new SimpleDataSourceProviderRegistry(
                    securityContext,
                    dataSourceUrlConfig);
        }
    }

    @Override
    public Optional<DataSourceProvider> getDataSourceProvider(final DocRef dataSourceRef) {
        return delegateDataSourceProviderRegistry.getDataSourceProvider(dataSourceRef);
    }
}
