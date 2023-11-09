package stroom.dropwizard.common;

import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Set;
import jakarta.inject.Inject;

public class ManagedServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedServices.class);

    private final Environment environment;
    private final Set<Managed> managedServices;

    @Inject
    ManagedServices(final Environment environment, final Set<Managed> managedServices) {
        this.environment = environment;
        this.managedServices = managedServices;
    }

    public void register() {
        LOGGER.info("Adding managed services:");
        managedServices.stream()
                .sorted(Comparator.comparing(managedService -> managedService.getClass().getName()))
                .forEach(managed -> {
                    final String name = managed.getClass().getName();
                    LOGGER.info("\t{}", name);
                    environment.lifecycle().manage(managed);
                });
    }
}
