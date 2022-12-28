package stroom.security.identity.openid;

import stroom.security.identity.config.IdentityConfig;
import stroom.security.openid.api.IdpType;
import stroom.security.openid.api.OpenIdClient;
import stroom.security.openid.api.OpenIdClientFactory;
import stroom.security.openid.api.OpenIdConfiguration;
import stroom.util.authentication.DefaultOpenIdCredentials;
import stroom.util.logging.LogUtil;

import java.security.SecureRandom;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Provider;

public class OpenIdClientDetailsFactoryImpl implements OpenIdClientFactory {

    private static final String INTERNAL_STROOM_CLIENT = "Stroom Client Internal";
    private static final String CLIENT_ID_SUFFIX = ".client-id.apps.stroom-idp";
    private static final String CLIENT_SECRET_SUFFIX = ".client-secret.apps.stroom-idp";
    private static final char[] ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPRSTUVWXYZ0123456789"
            .toCharArray();
    private static final int ALLOWED_CHARS_COUNT = ALLOWED_CHARS.length;

    private final DefaultOpenIdCredentials defaultOpenIdCredentials;
    private final Provider<OpenIdConfiguration> openIdConfigurationProvider;
    private final OpenIdClient oAuth2Client;

    @Inject
    public OpenIdClientDetailsFactoryImpl(final OpenIdClientDao dao,
                                          final IdentityConfig authenticationConfig,
                                          final DefaultOpenIdCredentials defaultOpenIdCredentials,
                                          final Provider<OpenIdConfiguration> openIdConfigurationProvider) {
        this.defaultOpenIdCredentials = defaultOpenIdCredentials;
        this.openIdConfigurationProvider = openIdConfigurationProvider;

        // TODO The way this is implemented means we are limited to a single client when using our
        //   internal auth provider.  Not sure this is what we want when we have stroom-stats in the
        //   mix. However to manage multiple client IDs we would probably need UI pages to do the CRUD on them.

        final OpenIdClient oAuth2Client;
        if (IdpType.TEST_CREDENTIALS.equals(openIdConfigurationProvider.get().getIdentityProviderType())) {
            oAuth2Client = createDefaultOAuthClient();
        } else {
            oAuth2Client = dao.getClientByName(INTERNAL_STROOM_CLIENT)
                    .or(() -> {
                        // Generate new randomised client details and persist them
                        final OpenIdClient newOAuth2Client = createRandomisedOAuth2Client(INTERNAL_STROOM_CLIENT);
                        dao.create(newOAuth2Client);
                        return dao.getClientByName(INTERNAL_STROOM_CLIENT);
                    })
                    .orElseThrow(() ->
                            new NullPointerException("Unable to get or create internal client details"));
        }
        this.oAuth2Client = oAuth2Client;
    }

    public OpenIdClient getClient() {
        return oAuth2Client;
    }

    public OpenIdClient getClient(final String clientId) {
        // TODO currently only support one client ID so just have to throw if the client id is wrong
        if (!Objects.requireNonNull(clientId).equals(oAuth2Client.getClientId())) {
            throw new RuntimeException(LogUtil.message(
                    "Unexpected client ID: {}, expecting {}", clientId, oAuth2Client.getClientId()));
        }
        return oAuth2Client;
    }

    private OpenIdClient createDefaultOAuthClient() {
        return new OpenIdClient(
                defaultOpenIdCredentials.getOauth2ClientName(),
                defaultOpenIdCredentials.getOauth2ClientId(),
                defaultOpenIdCredentials.getOauth2ClientSecret(),
                defaultOpenIdCredentials.getOauth2ClientUriPattern());
    }

    static OpenIdClient createRandomisedOAuth2Client(final String name) {
        return new OpenIdClient(
                name,
                createRandomCode(40) + CLIENT_ID_SUFFIX,
                createRandomCode(40) + CLIENT_SECRET_SUFFIX,
                ".*");
    }

    private static String createRandomCode(final int length) {
        final SecureRandom secureRandom = new SecureRandom();
        final StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            stringBuilder.append(ALLOWED_CHARS[secureRandom.nextInt(ALLOWED_CHARS_COUNT)]);
        }
        return stringBuilder.toString();
    }
}
