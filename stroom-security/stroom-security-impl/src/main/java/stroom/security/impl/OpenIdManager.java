package stroom.security.impl;

import stroom.security.api.UserIdentity;
import stroom.security.impl.exception.AuthenticationException;
import stroom.security.openid.api.OpenId;
import stroom.security.openid.api.TokenRequest;
import stroom.security.openid.api.TokenResponse;
import stroom.security.shared.User;
import stroom.util.authentication.DefaultOpenIdCredentials;
import stroom.util.io.StreamUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.net.UrlUtils;
import stroom.util.servlet.UserAgentSessionUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

class OpenIdManager {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(OpenIdManager.class);

    private final ResolvedOpenIdConfig openIdConfig;
    private final DefaultOpenIdCredentials defaultOpenIdCredentials;
    private final JwtContextFactory jwtContextFactory;
    private final UserCache userCache;
    private final Provider<CloseableHttpClient> httpClientProvider;

    @Inject
    public OpenIdManager(final ResolvedOpenIdConfig openIdConfig,
                         final DefaultOpenIdCredentials defaultOpenIdCredentials,
                         final JwtContextFactory jwtContextFactory,
                         final UserCache userCache,
                         final Provider<CloseableHttpClient> httpClientProvider) {
        this.openIdConfig = openIdConfig;
        this.defaultOpenIdCredentials = defaultOpenIdCredentials;
        this.jwtContextFactory = jwtContextFactory;
        this.userCache = userCache;
        this.httpClientProvider = httpClientProvider;
    }

    public String redirect(final HttpServletRequest request,
                           final String code,
                           final String stateId,
                           final String postAuthRedirectUri) {
        final String uri = OpenId.removeReservedParams(postAuthRedirectUri);
        String redirectUri = null;

        // If we have completed the front channel flow then we will have a state id.
        if (code != null && stateId != null) {
            redirectUri = backChannelOIDC(request, code, stateId, uri);
        }

        if (redirectUri == null) {
            redirectUri = frontChannelOIDC(request, uri);
        }

        return redirectUri;
    }

    private String frontChannelOIDC(final HttpServletRequest request, final String postAuthRedirectUri) {
        final String endpoint = openIdConfig.getAuthEndpoint();
        final String clientId = openIdConfig.getClientId();
        Objects.requireNonNull(endpoint,
                "To make an authentication request the OpenId config 'authEndpoint' must not be null");
        Objects.requireNonNull(clientId,
                "To make an authentication request the OpenId config 'clientId' must not be null");
        // Create a state for this authentication request.
        final AuthenticationState state = AuthenticationStateSessionUtil.create(request, postAuthRedirectUri);
        LOGGER.debug(() -> "frontChannelOIDC state=" + state);
        return createAuthUri(request, endpoint, clientId, postAuthRedirectUri, state, false);
    }

    private String backChannelOIDC(final HttpServletRequest request,
                                   final String code,
                                   final String stateId,
                                   final String postAuthRedirectUri) {
        Objects.requireNonNull(code, "Null code");
        Objects.requireNonNull(stateId, "Null state Id");

        boolean loggedIn = false;
        String redirectUri = null;

        // If we have a state id then this should be a return from the auth service.
        LOGGER.debug(() -> "We have the following state: " + stateId);

        // Check the state is one we requested.
        final AuthenticationState state = AuthenticationStateSessionUtil.pop(request, stateId);
        if (state == null) {
            LOGGER.warn(() -> "Unexpected state: " + stateId);

        } else {
            LOGGER.debug(() -> "backChannelOIDC state=" + state);

            // Invalidate the current session.
            final HttpSession session = request.getSession(false);
            UserAgentSessionUtil.set(request);

            final ObjectMapper mapper = getMapper();
            final String tokenEndpoint = openIdConfig.getTokenEndpoint();
            final HttpPost httpPost = new HttpPost(tokenEndpoint);

            // AWS requires form content and not a JSON object.
            if (openIdConfig.isFormTokenRequest()) {
                final List<NameValuePair> nvps = new ArrayList<>();
                nvps.add(new BasicNameValuePair(OpenId.CODE, code));
                nvps.add(new BasicNameValuePair(OpenId.GRANT_TYPE, OpenId.GRANT_TYPE__AUTHORIZATION_CODE));
                nvps.add(new BasicNameValuePair(OpenId.CLIENT_ID, openIdConfig.getClientId()));
                nvps.add(new BasicNameValuePair(OpenId.CLIENT_SECRET, openIdConfig.getClientSecret()));
                nvps.add(new BasicNameValuePair(OpenId.REDIRECT_URI, postAuthRedirectUri));
                setFormParams(httpPost, nvps);

            } else {
                try {
                    final TokenRequest tokenRequest = TokenRequest.builder()
                            .code(code)
                            .grantType(OpenId.GRANT_TYPE__AUTHORIZATION_CODE)
                            .clientId(openIdConfig.getClientId())
                            .clientSecret(openIdConfig.getClientSecret())
                            .redirectUri(postAuthRedirectUri)
                            .build();
                    final String json = mapper.writeValueAsString(tokenRequest);

                    httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
                } catch (final JsonProcessingException e) {
                    throw new AuthenticationException(e.getMessage(), e);
                }
            }

            final TokenResponse tokenResponse = getTokenResponse(mapper, httpPost, tokenEndpoint);
            final Optional<JwtContext> optionalJwtContext = jwtContextFactory.getJwtContext(tokenResponse.getIdToken());
            final JwtClaims jwtClaims = optionalJwtContext
                    .map(JwtContext::getJwtClaims)
                    .orElseThrow(() -> new RuntimeException("Unable to extract JWT claims"));

            final UserIdentityImpl token = createUIToken(
                    session,
                    state,
                    tokenResponse,
                    jwtClaims);
            if (token != null) {
                // Set the token in the session.
                UserIdentitySessionUtil.set(session, token);
                loggedIn = true;
            }

            // If we manage to login then redirect to the original URL held in the state.
            if (loggedIn) {
                LOGGER.info(() -> "Redirecting to initiating URL: " + state.getUrl());
                redirectUri = state.getUrl();
            }
        }

        return redirectUri;
    }

    public void refreshToken(final UserIdentityImpl userIdentity) {
        LOGGER.debug("Refreshing token " + userIdentity);

        if (userIdentity.getTokenResponse().getRefreshToken() == null) {
            throw new NullPointerException("Unable to refresh token as no refresh token is available");
        }

        final ObjectMapper mapper = getMapper();
        final String tokenEndpoint = openIdConfig.getTokenEndpoint();
        final HttpPost httpPost = new HttpPost(tokenEndpoint);

        // AWS requires form content and not a JSON object.
        if (openIdConfig.isFormTokenRequest()) {
            final List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair(OpenId.GRANT_TYPE, OpenId.REFRESH_TOKEN));
            nvps.add(new BasicNameValuePair(OpenId.REFRESH_TOKEN,
                    userIdentity.getTokenResponse().getRefreshToken()));
            nvps.add(new BasicNameValuePair(OpenId.CLIENT_ID, openIdConfig.getClientId()));
            nvps.add(new BasicNameValuePair(OpenId.CLIENT_SECRET, openIdConfig.getClientSecret()));
            setFormParams(httpPost, nvps);

        } else {
            throw new UnsupportedOperationException("JSON not supported for token refresh");
        }

        final TokenResponse tokenResponse = getTokenResponse(mapper, httpPost, tokenEndpoint);
        final Optional<JwtContext> optionalJwtContext = jwtContextFactory.getJwtContext(tokenResponse.getIdToken());
        final JwtClaims jwtClaims = optionalJwtContext
                .map(JwtContext::getJwtClaims)
                .orElseThrow(() -> new RuntimeException("Unable to extract JWT claims"));

        userIdentity.setTokenResponse(tokenResponse);
        userIdentity.setJwtClaims(jwtClaims);
    }

    private void setFormParams(final HttpPost httpPost,
                               final List<NameValuePair> nvps) {
        try {
            String authorization = openIdConfig.getClientId() + ":" + openIdConfig.getClientSecret();
            authorization = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
            authorization = "Basic " + authorization;

            httpPost.setHeader(HttpHeaders.AUTHORIZATION, authorization);
            httpPost.setHeader(HttpHeaders.ACCEPT, "*/*");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        } catch (final UnsupportedEncodingException e) {
            throw new AuthenticationException(e.getMessage(), e);
        }
    }

    private TokenResponse getTokenResponse(final ObjectMapper mapper,
                                           final HttpPost httpPost,
                                           final String tokenEndpoint) {
        TokenResponse tokenResponse = null;
        try (final CloseableHttpClient httpClient = httpClientProvider.get()) {
            try (final CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (HttpServletResponse.SC_OK == response.getStatusLine().getStatusCode()) {
                    final String msg = getMessage(response);
                    tokenResponse = mapper.readValue(msg, TokenResponse.class);
                } else {
                    throw new AuthenticationException("Received status " +
                            response.getStatusLine() +
                            " from " +
                            tokenEndpoint);
                }
            }
        } catch (final IOException e) {
            LOGGER.debug(e::getMessage, e);
        }

        if (tokenResponse == null || tokenResponse.getIdToken() == null) {
            throw new AuthenticationException("'" +
                    OpenId.ID_TOKEN +
                    "' not provided in response");
        }

        return tokenResponse;
    }

    private String getMessage(final CloseableHttpResponse response) {
        String msg = "";
        try {
            final HttpEntity entity = response.getEntity();
            try (final InputStream is = entity.getContent()) {
                msg = StreamUtil.streamToString(is);
            }
        } catch (final RuntimeException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return msg;
    }

    private ObjectMapper getMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private UserIdentityImpl createUIToken(final HttpSession session,
                                           final AuthenticationState state,
                                           final TokenResponse tokenResponse,
                                           final JwtClaims jwtClaims) {
        UserIdentityImpl token = null;

        final String nonce = (String) jwtClaims.getClaimsMap().get(OpenId.NONCE);
        final boolean match = nonce != null && nonce.equals(state.getNonce());
        if (match) {
            final String sessionId = session.getId();
            LOGGER.info(() -> "User is authenticated for sessionId " + sessionId);
            final String userId = getUserId(jwtClaims);
            final Optional<User> optionalUser = userCache.get(userId);
            final User user = optionalUser.orElseThrow(() ->
                    new AuthenticationException("Unable to find user: " + userId));
            token = new UserIdentityImpl(user.getUuid(), userId, sessionId, tokenResponse, jwtClaims);

        } else {
            // If the nonces don't match we need to redirect to log in again.
            // Maybe the request uses an out-of-date stroomSessionId?
            LOGGER.info(() -> "Received a bad nonce!");
        }

        return token;
    }

    private String getUserId(final JwtClaims jwtClaims) {
        LOGGER.trace("getUserId");
        String userId = JwtUtil.getEmail(jwtClaims);
        if (userId == null) {
            userId = JwtUtil.getUserIdFromIdentities(jwtClaims);
        }
        if (userId == null) {
            userId = JwtUtil.getUserName(jwtClaims);
        }
        if (userId == null) {
            userId = JwtUtil.getSubject(jwtClaims);
        }

        return userId;
    }

    /**
     * This method attempts to get a token from the request headers and, if present, use that to login.
     */
    public Optional<UserIdentity> loginWithRequestToken(final HttpServletRequest request) {
        Optional<UserIdentity> userIdentity = Optional.empty();

        // See if we can login with a token if one is supplied.
        try {
            final Optional<JwtContext> optionalJwtContext = jwtContextFactory.getJwtContext(request);
            if (optionalJwtContext.isPresent()) {
                userIdentity = optionalJwtContext.flatMap(jwtContext -> getUserIdentity(request, jwtContext));
            } else {
                LOGGER.debug(() -> "No JWS found in headers in request to " + request.getRequestURI());
            }
        } catch (final RuntimeException e) {
            LOGGER.debug(e::getMessage, e);
        }

        if (userIdentity.isEmpty()) {
            LOGGER.debug(() -> "Cannot get a valid JWS for API request to " + request.getRequestURI() + ". " +
                    "This may be due to Stroom being left open in a browser after Stroom was restarted.");
        }

        return userIdentity;
    }

    public Optional<UserIdentity> getOrSetSessionUser(final HttpServletRequest request,
                                                       final Optional<UserIdentity> userIdentity) {
        Optional<UserIdentity> result = userIdentity;

        if (userIdentity.isEmpty()) {
            // Provide identity from the session if we are allowing this to happen.
            result = UserIdentitySessionUtil.get(request.getSession(false));

        } else if (UserIdentitySessionUtil.requestHasSessionCookie(request)) {
            // Set the user ref in the session.
            UserIdentitySessionUtil.set(request.getSession(true), userIdentity.get());
        }

        return result;
    }

    private Optional<UserIdentity> getUserIdentity(final HttpServletRequest request,
                                                   final JwtContext jwtContext) {
        LOGGER.debug(() -> "Getting user identity from jwtContext=" + jwtContext);

        String sessionId = null;
        final HttpSession session = request.getSession(false);
        if (session != null) {
            sessionId = session.getId();
        }

        try {
            final String userId = getUserId(jwtContext.getJwtClaims());
            final User user;
            if (jwtContext.getJwtClaims().getAudience().contains(defaultOpenIdCredentials.getOauth2ClientId())
                    && userId.equals(defaultOpenIdCredentials.getApiKeyUserEmail())) {
                LOGGER.warn(() ->
                        "Authenticating using default API key. For production use, set up an API key in Stroom!");
                // Using default creds so just fake a user
                // TODO Not sure if this is enough info in the user
                user = new User();
                user.setName(userId);
                user.setUuid(UUID.randomUUID().toString());
            } else {
                user = userCache.get(userId).orElseThrow(() ->
                        new AuthenticationException("Unable to find user: " + userId));
            }

            return Optional.of(new ApiUserIdentity(user.getUuid(),
                    userId,
                    sessionId,
                    jwtContext));

        } catch (final MalformedClaimException e) {
            LOGGER.error(() -> "Error extracting claims from token in request " + request.getRequestURI());
            return Optional.empty();
        }
    }

    public String logout(final HttpServletRequest request, final String postAuthRedirectUri) {
        final String redirectUri = OpenId.removeReservedParams(postAuthRedirectUri);
        final String endpoint = openIdConfig.getLogoutEndpoint();
        final String clientId = openIdConfig.getClientId();
        Objects.requireNonNull(endpoint,
                "To make a logout request the OpenId config 'logoutEndpoint' must not be null");
        Objects.requireNonNull(clientId,
                "To make an authentication request the OpenId config 'clientId' must not be null");
        final AuthenticationState state = AuthenticationStateSessionUtil.create(request, redirectUri);
        LOGGER.debug(() -> "logout state=" + state);
        return createAuthUri(request, endpoint, clientId, redirectUri, state, true);
    }

    private String createAuthUri(final HttpServletRequest request,
                                 final String endpoint,
                                 final String clientId,
                                 final String redirectUri,
                                 final AuthenticationState state,
                                 final boolean prompt) {

        // In some cases we might need to use an external URL as the current incoming one might have been proxied.
        // Use OIDC API.
        UriBuilder authenticationRequest = UriBuilder.fromUri(endpoint)
                .queryParam(OpenId.RESPONSE_TYPE, OpenId.CODE)
                .queryParam(OpenId.CLIENT_ID, clientId)
                .queryParam(OpenId.REDIRECT_URI, redirectUri)
                .queryParam(OpenId.SCOPE,
                        OpenId.SCOPE__OPENID + " " + OpenId.SCOPE__EMAIL + " " + OpenId.SCOPE__OFFLINE_ACCESS)
                .queryParam(OpenId.STATE, state.getId())
                .queryParam(OpenId.NONCE, state.getNonce());

        // If there's 'prompt' in the request then we'll want to pass that on to the AuthenticationService.
        // In OpenId 'prompt=login' asks the IP to present a login page to the user, and that's the effect
        // this will have. We need this so that we can bypass certificate logins, e.g. for when we need to
        // log in as the 'admin' user but the browser is always presenting a certificate.
        final String promptParam = UrlUtils.getLastParam(request, OpenId.PROMPT);
        if (!Strings.isNullOrEmpty(promptParam)) {
            authenticationRequest.queryParam(OpenId.PROMPT, promptParam);
        } else if (prompt) {
            authenticationRequest.queryParam(OpenId.PROMPT, "login");
        }

        final String authenticationRequestUrl = authenticationRequest.build().toString();
        LOGGER.info(() -> "Redirecting with an AuthenticationRequest to: " + authenticationRequestUrl);
        // We want to make sure that the client has the cookie.
        return authenticationRequestUrl;
    }
}
