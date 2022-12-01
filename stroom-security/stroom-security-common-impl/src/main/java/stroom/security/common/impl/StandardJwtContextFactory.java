package stroom.security.common.impl;

import stroom.security.openid.api.OpenIdConfiguration;
import stroom.security.api.exception.AuthenticationException;
import stroom.util.NullSafe;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

public class StandardJwtContextFactory implements JwtContextFactory {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(StandardJwtContextFactory.class);

    private static final String AMZN_OIDC_ACCESS_TOKEN_HEADER = "x-amzn-oidc-accesstoken";
    private static final String AMZN_OIDC_IDENTITY_HEADER = "x-amzn-oidc-identity";
    private static final String AMZN_OIDC_DATA_HEADER = "x-amzn-oidc-data";
    private static final String AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION;

    private final Provider<OpenIdConfiguration> openIdConfigurationProvider;
    private final OpenIdPublicKeysSupplier openIdPublicKeysSupplier;

    @Inject
    public StandardJwtContextFactory(final Provider<OpenIdConfiguration> openIdConfigurationProvider,
                                     final OpenIdPublicKeysSupplier openIdPublicKeysSupplier) {
        this.openIdConfigurationProvider = openIdConfigurationProvider;
        this.openIdPublicKeysSupplier = openIdPublicKeysSupplier;
    }

    @Override
    public boolean hasToken(final HttpServletRequest request) {
        return getTokenFromHeader(request)
                .isPresent();
    }

    @Override
    public void removeAuthorisationEntries(final Map<String, String> headers) {
        if (NullSafe.hasEntries(headers)) {
            headers.remove(AUTHORIZATION_HEADER);
            headers.remove(AMZN_OIDC_ACCESS_TOKEN_HEADER);
            headers.remove(AMZN_OIDC_DATA_HEADER);
            headers.remove(AMZN_OIDC_IDENTITY_HEADER);
        }
    }

    @Override
    public Optional<JwtContext> getJwtContext(final HttpServletRequest request) {
        LOGGER.debug(() -> AMZN_OIDC_ACCESS_TOKEN_HEADER + "=" + request.getHeader(AMZN_OIDC_ACCESS_TOKEN_HEADER));
        LOGGER.debug(() -> AMZN_OIDC_IDENTITY_HEADER + "=" + request.getHeader(AMZN_OIDC_IDENTITY_HEADER));
        LOGGER.debug(() -> AMZN_OIDC_DATA_HEADER + "=" + request.getHeader(AMZN_OIDC_DATA_HEADER));
        LOGGER.debug(() -> AUTHORIZATION_HEADER + "=" + request.getHeader(AUTHORIZATION_HEADER));

        final Optional<String> optionalJwt = getTokenFromHeader(request);
        return optionalJwt
                .flatMap(this::getJwtContext)
                .or(() -> {
                    LOGGER.debug(() -> "No JWS found in headers in request to " + request.getRequestURI());
                    return Optional.empty();
                });
    }

    private Optional<String> getTokenFromHeader(final HttpServletRequest request) {
        return JwtUtil.getJwsFromHeader(request, AMZN_OIDC_DATA_HEADER)
                .or(() -> JwtUtil.getJwsFromHeader(request, AUTHORIZATION_HEADER));
    }

    /**
     * Verify the JSON Web Signature and then extract the user identity from it
     */
    @Override
    public Optional<JwtContext> getJwtContext(final String jwt) {
        Objects.requireNonNull(jwt, "Null JWS");
        LOGGER.debug(() -> "Found auth header in request. It looks like this: " + jwt);

        try {
            LOGGER.debug(() -> "Verifying token...");
            final JwtConsumer jwtConsumer = newJwtConsumer();
            final JwtContext jwtContext = jwtConsumer.process(jwt);

            // TODO : @66 Check against blacklist to see if token has been revoked. Blacklist
            //  is a list of JWI (JWT IDs) on auth service. Only tokens with `jwi` claims are API
            //  keys so only those tokens need checking against the blacklist cache.

//            if (checkTokenRevocation) {
//                LOGGER.debug(() -> "Checking token revocation status in remote auth service...");
//                final String userId = getUserIdFromToken(jws);
//                isRevoked = userId == null;
//            }

            return Optional.ofNullable(jwtContext);

        } catch (final RuntimeException | InvalidJwtException e) {
            LOGGER.debug(() -> "Unable to verify token: " + e.getMessage(), e);
            throw new AuthenticationException(e.getMessage(), e);
        }
    }

    private JwtConsumer newJwtConsumer() {
        // If we don't have a JWK we can't create a consumer to verify anything.
        // Why might we not have one? If the remote authentication service was down when Stroom started
        // then we wouldn't. It might not be up now but we're going to try and fetch it.
        final JsonWebKeySet publicJsonWebKey = openIdPublicKeysSupplier.get();

        final VerificationKeyResolver verificationKeyResolver = new JwksVerificationKeyResolver(
                publicJsonWebKey.getJsonWebKeys());
        final OpenIdConfiguration openIdConfiguration = openIdConfigurationProvider.get();

        final JwtConsumerBuilder builder = new JwtConsumerBuilder()
                .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account
                //                                   for clock skew
                .setRequireSubject() // the JWT must have a subject claim
                .setVerificationKeyResolver(verificationKeyResolver)
                .setRelaxVerificationKeyValidation() // relaxes key length requirement
//                .setJwsAlgorithmConstraints(// only allow the expected signature algorithm(s) in the given context
//                        new AlgorithmConstraints(
//                                AlgorithmConstraints.ConstraintType.WHITELIST, // which is only RS256 here
//                                AlgorithmIdentifiers.RSA_USING_SHA256))
                .setExpectedIssuer(openIdConfiguration.getIssuer());

        if (openIdConfiguration.isValidateAudience()) {
            // aud does not appear in access tokens by default it seems so make the check optional
                builder.setExpectedAudience(openIdConfiguration.getClientId());
        }
        return builder.build();
    }
}
