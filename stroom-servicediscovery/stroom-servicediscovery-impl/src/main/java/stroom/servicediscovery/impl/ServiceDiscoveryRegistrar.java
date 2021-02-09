package stroom.servicediscovery.impl;

import stroom.servicediscovery.api.RegisteredService;
import stroom.util.HasHealthCheck;
import stroom.util.shared.ResourcePaths;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheck.Result;
import com.google.common.base.Preconditions;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.curator.x.discovery.UriSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Responsible for registering stroom's various externally exposed services with service discovery
 */
@Singleton
public class ServiceDiscoveryRegistrar implements HasHealthCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscoveryRegistrar.class);

    private final ServiceDiscoveryManager serviceDiscoveryManager;
    private final String hostNameOrIpAddress;
    private final int servicePort;
    private HealthCheck.Result health;

    @Inject
    ServiceDiscoveryRegistrar(final ServiceDiscoveryManager serviceDiscoveryManager,
                              final ServiceDiscoveryConfig serviceDiscoveryConfig) {
        this.serviceDiscoveryManager = serviceDiscoveryManager;
        this.hostNameOrIpAddress = getHostOrIp(serviceDiscoveryConfig);
        this.servicePort = serviceDiscoveryConfig.getServicesPort();

        if (serviceDiscoveryConfig.isEnabled()) {
            health = HealthCheck.Result.unhealthy("Not yet initialised...");
            this.serviceDiscoveryManager.registerStartupListener(this::curatorStartupListener);
        } else {
            health = HealthCheck.Result.healthy("Service discovery is disabled.");
        }
    }

    private String getHostOrIp(final ServiceDiscoveryConfig serviceDiscoveryConfig) {
        String hostOrIp = serviceDiscoveryConfig.getServicesHostNameOrIpAddress();
        if (hostOrIp == null || hostOrIp.isEmpty()) {
            try {
                hostOrIp = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                LOGGER.warn("Unable to determine hostname of this instance due to error. Will try to get IP address instead", e);
            }

            if (hostOrIp == null || hostOrIp.isEmpty()) {
                try {
                    hostOrIp = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(String.format("Error establishing hostname or IP address of this instance"), e);
                }
            }
        }
        return hostOrIp;
    }

    private void curatorStartupListener(ServiceDiscovery<String> serviceDiscovery) {
        try {
            Map<String, String> services = new TreeMap<>();
            Arrays.stream(RegisteredService.values())
                    .forEach(registeredService -> {
                        ServiceInstance<String> serviceInstance = registerResource(
                                registeredService,
                                serviceDiscovery);
                        services.put(registeredService.getVersionedServiceName(), serviceInstance.buildUriSpec());
                    });

            health = HealthCheck.Result.builder()
                    .healthy()
                    .withMessage("Local services registered")
                    .withDetail("registered-services", services)
                    .build();

            LOGGER.info("All service instances created successfully.");
        } catch (final RuntimeException e) {
            health = HealthCheck.Result.unhealthy("Service instance creation failed!", e);
            LOGGER.error("Service instance creation failed!", e);
            throw new RuntimeException("Service instance creation failed!", e);
        }
    }

    private ServiceInstance<String> registerResource(final RegisteredService registeredService,
                                                     final ServiceDiscovery<String> serviceDiscovery) {
        try {
            final UriSpec uriSpec = new UriSpec("{scheme}://{address}:{port}" +
                    ResourcePaths.buildAuthenticatedApiPath(registeredService.getVersionedPath()));

            final ServiceInstance<String> serviceInstance = ServiceInstance.<String>builder()
                    .serviceType(ServiceType.DYNAMIC) //==ephemeral zk nodes so instance will disappear if we lose zk conn
                    .uriSpec(uriSpec)
                    .name(registeredService.getVersionedServiceName())
                    .address(hostNameOrIpAddress)
                    .port(servicePort)
                    .build();

            LOGGER.info("Attempting to register '{}' with service discovery at {}",
                    registeredService.getVersionedServiceName(), serviceInstance.buildUriSpec());

            Preconditions.checkNotNull(serviceDiscovery).registerService(serviceInstance);

            LOGGER.info("Successfully registered '{}' service.", registeredService.getVersionedServiceName());
            return serviceInstance;
        } catch (final Exception e) {
            throw new RuntimeException("Failed to register service " + registeredService.getVersionedServiceName(), e);
        }
    }

    @Override
    public Result getHealth() {
        return health;
    }
}
