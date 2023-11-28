package stroom.app.commands;

import stroom.app.guice.AppModule;
import stroom.config.app.Config;
import stroom.security.api.SecurityContext;
import stroom.util.exception.LambdaExceptionUtil;
import stroom.util.guice.GuiceUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

import java.nio.file.Path;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Abstract command that sets up a full application injector with access to all bindings.
 */
public abstract class AbstractStroomAppCommand extends AbstractStroomBaseCommand {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(AbstractStroomAppCommand.class);

    public AbstractStroomAppCommand(final Path configFile,
                                    final String commandName,
                                    final String description) {
        super(configFile, commandName, description);
    }

    @Override
    protected void runCommand(final Bootstrap<Config> bootstrap,
                              final Namespace namespace,
                              final Config config,
                              final Injector injector) throws Exception {

        // Force guice to get all datasource instances from the multibinder
        // so the migration will be run for each stroom module
        // Relies on all db modules adding an entry to the multibinder
        getInstance(Key.get(GuiceUtil.setOf(DataSource.class)));

        LOGGER.debug("DB schema state verified");

        final SecurityContext securityContext = getInstance(SecurityContext.class);

        securityContext.asProcessingUser(LambdaExceptionUtil.rethrowRunnable(() ->
                runSecuredCommand(bootstrap, namespace, config, injector)));
    }

    /**
     * Run the command as the processing user
     */
    protected abstract void runSecuredCommand(final Bootstrap<Config> bootstrap,
                                              final Namespace namespace,
                                              final Config config,
                                              final Injector injector) throws Exception;

    @Override
    protected Optional<Module> getChildInjectorModule() {
        return Optional.of(new AppModule());
    }
}
