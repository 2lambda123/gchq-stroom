package stroom.security.impl;

import stroom.security.api.UserIdentity;
import stroom.security.common.impl.AuthenticationState;
import stroom.security.common.impl.UserIdentitySessionUtil;
import stroom.security.openid.api.OpenId;
import stroom.security.openid.api.OpenIdConfiguration;
import stroom.util.NullSafe;
import stroom.util.jersey.UriBuilderUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.servlet.UserAgentSessionUtil;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.UriBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

class OpenIdManager {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(OpenIdManager.class);

    private final OpenIdConfiguration openIdConfiguration;
    // We have to use the stroom specific one as only that one has the code flow
    private final StroomUserIdentityFactory userIdentityFactory;

    @Inject
    public OpenIdManager(final OpenIdConfiguration openIdConfiguration,
                         final StroomUserIdentityFactory userIdentityFactory) {
        this.openIdConfiguration = openIdConfiguration;
        this.userIdentityFactory = userIdentityFactory;
    }

    public String redirect(final HttpServletRequest request,
                           final String code,
                           final String stateId,
                           final String postAuthRedirectUri) {
        String redirectUri = null;

        // Retrieve state if we have a state id param.
        final AuthenticationState state = getState(request, stateId);

        // If we have completed the front channel flow then we will have a code and state.
        if (code != null && state != null) {
            redirectUri = backChannelOIDC(request, code, state);
        }

        // If we aren't doing back channel check yet or the back channel check failed then proceed with front channel.
        if (redirectUri == null) {
            if (state != null) {
                // Restore the initiating URI as needed for logout.
                redirectUri = frontChannelOIDC(request, state.getInitiatingUri(), state.isPrompt());
            } else {
                redirectUri = frontChannelOIDC(request, postAuthRedirectUri, false);
            }
        }

        return redirectUri;
    }

    private AuthenticationState getState(final HttpServletRequest request, final String stateId) {
        AuthenticationState state = null;
        if (stateId != null) {
            // Check the state is one we requested.
            state = AuthenticationStateSessionUtil.pop(request, stateId);
            if (state == null) {
                LOGGER.debug("Unable to find state {}", stateId);
            } else {
                LOGGER.debug("Found state {} {}", stateId, state);
            }
        }
        return state;
    }

    private String frontChannelOIDC(final HttpServletRequest request,
                                    final String postAuthRedirectUri,
                                    final boolean prompt) {
        final String endpoint = openIdConfiguration.getAuthEndpoint();
        final String clientId = openIdConfiguration.getClientId();
        Objects.requireNonNull(endpoint,
                "To make an authentication request the OpenId config 'authEndpoint' must not be null");
        Objects.requireNonNull(clientId,
                "To make an authentication request the OpenId config 'clientId' must not be null");
        // Create a state for this authentication request.
        final AuthenticationState state = AuthenticationStateSessionUtil.create(request, postAuthRedirectUri, prompt);
        LOGGER.debug(() -> "frontChannelOIDC state: " + state);
//        return createAuthUri(request, endpoint, clientId, state, false, false);
        return createAuthUri(request, endpoint, clientId, state);
    }

    private String backChannelOIDC(final HttpServletRequest request,
                                   final String code,
                                   final AuthenticationState state) {
        Objects.requireNonNull(code, "Null code");

        boolean loggedIn = false;
        String redirectUri = null;

        // If we have a state id then this should be a return from the auth service.
        LOGGER.debug(() -> LogUtil.message("We have the following backChannelOIDC state: {}", state));

        final HttpSession session = request.getSession(false);
        UserAgentSessionUtil.set(request);

        final Optional<UserIdentity> optionalUserIdentity =
                userIdentityFactory.getAuthFlowUserIdentity(request, code, state);

        if (optionalUserIdentity.isPresent()) {
            // Set the token in the session.
            UserIdentitySessionUtil.set(session, optionalUserIdentity.get());
            loggedIn = true;
        }

        // If we manage to login then redirect to the original URL held in the state.
        if (loggedIn) {
            LOGGER.info(() -> "Redirecting to initiating URI: " + state.getInitiatingUri());
            redirectUri = state.getInitiatingUri();
        }

        return redirectUri;
    }

    /**
     * This method attempts to get a token from the request headers and, if present, use that to login.
     */
    public Optional<UserIdentity> loginWithRequestToken(final HttpServletRequest request) {
        if (userIdentityFactory.hasAuthenticationToken(request)) {
            return userIdentityFactory.getApiUserIdentity(request);
        } else {
            LOGGER.trace("No token on request. This is valid for API calls from the front-end");
            return Optional.empty();
        }
    }

    public Optional<UserIdentity> getOrSetSessionUser(final HttpServletRequest request,
                                                      final Optional<UserIdentity> userIdentity) {
        Optional<UserIdentity> result = userIdentity;

        if (userIdentity.isEmpty()) {
            // Provide identity from the session if we are allowing this to happen.
            result = UserIdentitySessionUtil.get(request.getSession(false));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("User identity from session: [{}]", result.orElse(null));
            }

        } else if (UserIdentitySessionUtil.requestHasSessionCookie(request)) {
            // Set the user ref in the session.
            UserIdentitySessionUtil.set(request.getSession(true), userIdentity.get());
        }

        return result;
    }

    public String logout(final HttpServletRequest request, final String postAuthRedirectUri) {
        final String endpoint = openIdConfiguration.getLogoutEndpoint();
        final String clientId = openIdConfiguration.getClientId();
        Objects.requireNonNull(endpoint,
                "To make a logout request the OpenId config 'logoutEndpoint' must not be null");
        Objects.requireNonNull(clientId,
                "To make an authentication request the OpenId config 'clientId' must not be null");
        final AuthenticationState state = AuthenticationStateSessionUtil.create(request, postAuthRedirectUri, true);
        LOGGER.debug(() -> "logout state=" + state);
//        return createAuthUri(request, endpoint, clientId, state, true, true);
        return createLogoutUri(endpoint, clientId, state);
    }

    private String createAuthUri(final HttpServletRequest request,
                                 final String endpoint,
                                 final String clientId,
                                 final AuthenticationState state) {
//                                final boolean isLogout) {

        // In some cases we might need to use an external URL as the current incoming one might have been proxied.
        // Use OIDC API.
        UriBuilder uriBuilder = UriBuilder.fromUri(endpoint);
        uriBuilder = UriBuilderUtil.addParam(uriBuilder, OpenId.RESPONSE_TYPE, OpenId.CODE);
        uriBuilder = UriBuilderUtil.addParam(uriBuilder, OpenId.CLIENT_ID, clientId);

//        final String redirectParamName = isLogout
//                ? openIdConfiguration.getLogoutRedirectParamName()
//                : OpenId.REDIRECT_URI;
//        uriBuilder = UriBuilderUtil.addParam(
//                uriBuilder,
//                redirectParamName,
//                state.getUri());
        uriBuilder = UriBuilderUtil.addParam(uriBuilder, OpenId.REDIRECT_URI, state.getRedirectUri());

        final List<String> requestScopes = openIdConfiguration.getRequestScopes();
        if (NullSafe.hasItems(requestScopes)) {
            uriBuilder = UriBuilderUtil.addParam(uriBuilder, OpenId.SCOPE, String.join(" ", requestScopes));
        }

        uriBuilder = UriBuilderUtil.addParam(uriBuilder, OpenId.STATE, state.getId());
        uriBuilder = UriBuilderUtil.addParam(uriBuilder, OpenId.NONCE, state.getNonce());

        // Determine if we want to force login regardless of IDP auth state.
        if (state.isPrompt()) {
            uriBuilder = UriBuilderUtil.addParam(uriBuilder, OpenId.PROMPT, OpenId.LOGIN_PROMPT);
        }

        final String authenticationRequestUrl = uriBuilder.build().toString();
        LOGGER.info(() -> "Redirecting with an AuthenticationRequest to: " + authenticationRequestUrl);
        // We want to make sure that the client has the cookie.
        return authenticationRequestUrl;
    }

    private String createLogoutUri(final String endpoint,
                                   final String clientId,
                                   final AuthenticationState state) {
        UriBuilder redirect = UriBuilder.fromUri(state.getRedirectUri());
        redirect = UriBuilderUtil.addParam(redirect, OpenId.STATE, state.getId());

        UriBuilder uriBuilder = UriBuilder.fromUri(endpoint);
        uriBuilder = UriBuilderUtil.addParam(uriBuilder, OpenId.CLIENT_ID, clientId);
        uriBuilder = UriBuilderUtil.addParam(uriBuilder, OpenId.POST_LOGOUT_REDIRECT_URI, redirect.build().toString());
        return uriBuilder.build().toString();
    }
}
