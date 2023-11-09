package stroom.app.commands;

import stroom.config.app.Config;
import stroom.event.logging.api.StroomEventLoggingService;
import stroom.security.api.SecurityContext;
import stroom.security.identity.account.Account;
import stroom.security.identity.account.AccountService;
import stroom.security.identity.token.ApiKey;
import stroom.security.identity.token.ApiKeyDao;
import stroom.security.identity.token.KeyType;
import stroom.security.identity.token.TokenBuilder;
import stroom.security.identity.token.TokenBuilderFactory;
import stroom.security.openid.api.OpenIdClientFactory;
import stroom.util.AuditUtil;
import stroom.util.logging.LogUtil;

import com.google.inject.Injector;
import event.logging.CreateEventAction;
import event.logging.Outcome;
import event.logging.User;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Set;
import jakarta.inject.Inject;

/**
 * Creates an API key for a user the internal identity provider
 */
public class CreateApiKeyCommand extends AbstractStroomAccountConfiguredCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateApiKeyCommand.class);
    private static final String COMMAND_NAME = "create_api_key";
    private static final String COMMAND_DESCRIPTION = "Creates an API key for a user in the local identity provider";

    private static final String USER_ID_ARG_NAME = "user";
    private static final String EXPIRY_DAYS_ARG_NAME = "expiresDays";
    private static final String OUTPUT_FILE_PATH_ARG_NAME = "outFile";
    private static final Set<String> ARGUMENT_NAMES = Set.of(
            USER_ID_ARG_NAME,
            EXPIRY_DAYS_ARG_NAME,
            OUTPUT_FILE_PATH_ARG_NAME);

    private final Path configFile;

    @Inject
    private TokenBuilderFactory tokenBuilderFactory;
    @Inject
    private OpenIdClientFactory openIdClientDetailsFactory;
    @Inject
    private ApiKeyDao apiKeyDao;
    @Inject
    private AccountService accountService;
    @Inject
    private SecurityContext securityContext;
    @Inject
    private StroomEventLoggingService stroomEventLoggingService;

    public CreateApiKeyCommand(final Path configFile) {
        super(configFile, COMMAND_NAME, COMMAND_DESCRIPTION);
        this.configFile = configFile;
    }

    @Override
    public void configure(final Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("-u", "--" + USER_ID_ARG_NAME)
                .dest(USER_ID_ARG_NAME)
                .type(String.class)
                .required(true)
                .help("The user id of the account to issue the API key against, e.g. 'admin'");

        subparser.addArgument("-e", "--" + EXPIRY_DAYS_ARG_NAME)
                .dest(EXPIRY_DAYS_ARG_NAME)
                .type(Integer.class)
                .required(false)
                .help("Expiry (in days) from the creation time");

        subparser.addArgument("-o", "--" + OUTPUT_FILE_PATH_ARG_NAME)
                .dest(OUTPUT_FILE_PATH_ARG_NAME)
                .type(String.class)
                .required(false)
                .help("Path to the file where the API key will be written");
    }

    @Override
    public Set<String> getArgumentNames() {
        return ARGUMENT_NAMES;
    }

    @Override
    protected void runCommand(final Bootstrap<Config> bootstrap,
                              final Namespace namespace,
                              final Config config,
                              final Injector injector) {

        injector.injectMembers(this);

        final String userId = namespace.getString(USER_ID_ARG_NAME);
        final String outputPath = namespace.getString(OUTPUT_FILE_PATH_ARG_NAME);

        accountService.read(userId)
                .ifPresentOrElse(
                        account -> {
                            final ApiKey apiKey = createApiKey(namespace, account);
                            if (outputApiKey(apiKey, outputPath)) {
                                final String msg = LogUtil.message("API key successfully created for user '{}'",
                                        userId);
                                LOGGER.info(msg);
                                System.exit(0);
                            } else {
                                final String msg = LogUtil.message("API key for user '{}' could not be output",
                                        userId);
                                logEvent(userId, true, msg);
                                System.exit(1);
                            }
                        },
                        () -> {
                            final String msg = LogUtil.message("Cannot issue API key as user account '{}' " +
                                    "does not exist", userId);
                            logEvent(userId, false, msg);
                            throw new RuntimeException(msg);
                        });
    }

    private ApiKey createApiKey(final Namespace namespace, final Account account) {
        final Integer lifetimeDays = namespace.getInt(EXPIRY_DAYS_ARG_NAME);
        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        LOGGER.info("Creating API key for user '{}'", account.getUserId());

        Instant expirationTime;
        if (lifetimeDays != null) {
            expirationTime = now.plusDays(lifetimeDays).toInstant(ZoneOffset.UTC);
        } else {
            expirationTime = new Date(Long.MAX_VALUE).toInstant();
        }

        final TokenBuilder tokenBuilder = tokenBuilderFactory
                .builder()
                .expirationTime(expirationTime)
                .clientId(openIdClientDetailsFactory.getClient().getClientId())
                .subject(account.getUserId());

        final ApiKey apiKey = new ApiKey();
        apiKey.setUserId(account.getUserId());
        apiKey.setUserEmail(account.getUserId());
        apiKey.setType(KeyType.API.getText());
        apiKey.setData(tokenBuilder.build());
        apiKey.setExpiresOnMs(tokenBuilder.getExpirationTime().toEpochMilli());
        apiKey.setEnabled(true);
        AuditUtil.stamp(securityContext, apiKey);

        // Register the API keu
        return apiKeyDao.create(account.getId(), apiKey);
    }

    /**
     * Emit the API key token data to the console, or if an output file path was specified, to that file.
     * If the file write fails, the token will be deleted.
     *
     * @return Whether the token was successfully written
     */
    private boolean outputApiKey(final ApiKey apiKey, final String path) {
        if (path != null) {
            // Output the API key to a file path specified by the CLI user
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(path));
                writer.write(apiKey.getData());
                writer.close();

                final File fileInfo = new File(path);
                LOGGER.info("Wrote API key for user '{}' to file '{}'", apiKey.getUserId(), fileInfo.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("API key for user '{}' could not be written to file. {}",
                        apiKey.getUserId(), e.getMessage());

                // Destroy the created API key
                apiKeyDao.deleteTokenById(apiKey.getId());

                return false;
            }
        } else {
            // Output API key to standard out
            LOGGER.info("Generated API key for user '{}': {}", apiKey.getUserId(), apiKey.getData());
        }

        return true;
    }

    private void logEvent(final String username,
                          final boolean wasSuccessful,
                          final String description) {

        stroomEventLoggingService.log(
                "CliCreateApiKey",
                LogUtil.message("An API key for user '{}' was created", username),
                CreateEventAction.builder()
                        .addUser(User.builder()
                                .withName(username)
                                .build())
                        .withOutcome(Outcome.builder()
                                .withSuccess(wasSuccessful)
                                .withDescription(description)
                                .build())
                        .build());
    }
}
