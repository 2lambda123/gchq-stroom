package stroom.servicediscovery.impl;

import stroom.servicediscovery.api.ExternalService;
import stroom.servicediscovery.api.ServiceDiscoverer;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheck.Result;
import io.vavr.Tuple2;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Singleton
public class ServiceDiscovererImpl implements ServiceDiscoverer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscovererImpl.class);

    private final Provider<ServiceDiscoveryConfig> serviceDiscoveryConfig;

    /*
    Note: When using Curator 2.x (Zookeeper 3.4.x) it's essential that service provider objects are cached by your
    application and reused. Since the internal NamespaceWatcher objects added by the service provider cannot be
    removed in Zookeeper 3.4.x, creating a fresh service provider for each call to the same service will
    eventually exhaust the memory of the JVM.
     */
    private final Map<ExternalService, ServiceProvider<String>> serviceProviders = new HashMap<>();

    @Inject
    ServiceDiscovererImpl(final Provider<ServiceDiscoveryConfig> serviceDiscoveryConfig,
                          final ServiceDiscoveryManager serviceDiscoveryManager) {
        //create the service providers once service discovery has started up
        this.serviceDiscoveryConfig = serviceDiscoveryConfig;
        serviceDiscoveryManager.registerStartupListener(this::initProviders);
    }

    @Override
    public Optional<ServiceInstance<String>> getServiceInstance(final ExternalService externalService) {
        LOGGER.trace("Getting service instance for {}", externalService.getServiceKey());
        return Optional.ofNullable(serviceProviders.get(externalService))
                .flatMap(stringServiceProvider -> {
                    try {
                        return Optional.ofNullable(stringServiceProvider.getInstance());
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public boolean isEnabled() {
        return serviceDiscoveryConfig.get().isEnabled();
    }

    private void initProviders(final ServiceDiscovery<String> serviceDiscovery) {

        //Attempt to create ServiceProviders for each of the ExternalServices
        Arrays.stream(ExternalService.values())
                .filter(externalService -> externalService.getType().equals(ExternalService.Type.CLIENT) ||
                        externalService.getType().equals(ExternalService.Type.CLIENT_AND_SERVER))
                .forEach(externalService -> {
                    ServiceProvider<String> serviceProvider = createProvider(serviceDiscovery, externalService);
                    LOGGER.debug("Adding service provider {}", externalService.getVersionedServiceName());
                    serviceProviders.put(externalService, serviceProvider);
                });
    }

    private ServiceProvider<String> createProvider(final ServiceDiscovery<String> serviceDiscovery,
                                                   final ExternalService externalService) {
        ServiceProvider<String> provider = serviceDiscovery.serviceProviderBuilder()
                .serviceName(externalService.getVersionedServiceName())
                .providerStrategy(externalService.getProviderStrategy())
                .build();
        try {
            provider.start();
        } catch (final Exception e) {
            LOGGER.error("Unable to start service provider for {}", externalService.getVersionedServiceName(), e);
        }

        return provider;
    }

    public void shutdown() {
        serviceProviders.forEach((key, value) -> {
            try {
                value.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close serviceProvider {} with error",
                        key.getVersionedServiceName(), e);
            }
        });
    }

    @Override
    public Result getHealth() {
        if (serviceDiscoveryConfig.get().isEnabled()) {
            if (serviceProviders.isEmpty()) {
                return HealthCheck.Result.unhealthy("No service providers found");
            } else {
                try {
                    Map<String, List<String>> serviceInstanceMap = serviceProviders.entrySet().stream()
                            .flatMap(entry -> {
                                try {
                                    return entry.getValue().getAllInstances().stream();
                                } catch (final Exception e) {
                                    throw new RuntimeException(String.format("Error querying instances for service %s",
                                            entry.getKey().getVersionedServiceName()), e);
                                }
                            })
                            .map(serviceInstance -> new Tuple2<>(serviceInstance.getName(),
                                    serviceInstance.buildUriSpec()))
                            .collect(Collectors.groupingBy(
                                    Tuple2::_1,
                                    TreeMap::new,
                                    Collectors.mapping(Tuple2::_2, Collectors.toList())));

                    //ensure the instances are sorted in a sensible way
                    serviceInstanceMap.values().forEach(Collections::sort);

                    long deadServiceCount = serviceInstanceMap.entrySet().stream()
                            .filter(entry -> entry.getValue().isEmpty())
                            .count();

                    HealthCheck.ResultBuilder builder = HealthCheck.Result.builder();

                    if (deadServiceCount > 0) {
                        builder.unhealthy()
                                .withMessage("%s service(s) have no registered instances");
                    } else {
                        builder.healthy()
                                .withMessage("All services (local and remote) available");
                    }
                    return builder.withDetail("discovered-service-instances", serviceInstanceMap)
                            .build();

                } catch (final RuntimeException e) {
                    return HealthCheck.Result.unhealthy("Error getting service provider details, error: " +
                            e.getCause().getMessage());
                }
            }
        } else {

            return HealthCheck.Result.healthy("Service discovery is disabled");
        }
    }
}
