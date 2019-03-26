/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.security.impl;

import com.google.common.base.Strings;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.auth.service.ApiException;
import stroom.security.api.Security;
import stroom.security.impl.exception.AuthenticationException;
import stroom.security.shared.UserRef;
import stroom.security.shared.UserToken;
import stroom.security.util.UserTokenUtil;
import stroom.ui.config.shared.UiConfig;
import stroom.util.io.StreamUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * <p>
 * Filter to avoid posts to the wrong place (e.g. the root of the app)
 * </p>
 */
@Singleton
class SecurityFilter implements Filter {
    private static final String IGNORE_URI_REGEX = "ignoreUri";

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityFilter.class);

    private final AuthenticationConfig config;
    private final UiConfig uiConfig;
    private final JWTService jwtService;
    private final AuthenticationServiceClients authenticationServiceClients;
    private final AuthenticationService authenticationService;
    private final Security security;

    private Pattern pattern = null;

    @Inject
    SecurityFilter(
            final AuthenticationConfig config,
            final UiConfig uiConfig,
            final JWTService jwtService,
            final AuthenticationServiceClients authenticationServiceClients,
            final AuthenticationService authenticationService,
            final Security security) {
        this.config = config;
        this.uiConfig = uiConfig;
        this.jwtService = jwtService;
        this.authenticationServiceClients = authenticationServiceClients;
        this.authenticationService = authenticationService;
        this.security = security;
    }

    @Override
    public void init(final FilterConfig filterConfig) {
        final String regex = filterConfig.getInitParameter(IGNORE_URI_REGEX);
        if (regex != null) {
            pattern = Pattern.compile(regex);
        }
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (!(response instanceof HttpServletResponse)) {
            final String message = "Unexpected response type: " + response.getClass().getName();
            LOGGER.error(message);
            return;
        }
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        if (!(request instanceof HttpServletRequest)) {
            final String message = "Unexpected request type: " + request.getClass().getName();
            LOGGER.error(message);
            httpServletResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
            return;
        }
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        filter(httpServletRequest, httpServletResponse, chain);
    }

    private void filter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        if (request.getMethod().toUpperCase().equals(HttpMethod.OPTIONS)) {
            // We need to allow CORS preflight requests
            chain.doFilter(request, response);

        } else if (ignoreUri(request.getRequestURI())) {
            // Allow some URIs to bypass authentication checks
            chain.doFilter(request, response);

        } else {
            // We need to distinguish between requests from an API client and from the UI.
            // - If a request is from the UI and fails authentication then we need to redirect to the login page.
            // - If a request is from an API client and fails authentication then we need to return HTTP 403 UNAUTHORIZED.
            // - If a request is for clustercall.rpc then it's a back-channel stroom-to-stroom request and we want to
            //   let it through. It is essential that port 8080 is not exposed and that any reverse-proxy
            //   blocks requests that look like '.*clustercall.rpc$'.
            final String servletPath = request.getServletPath().toLowerCase();
            final boolean isApiRequest = servletPath.contains("/api");
            final boolean isDatafeedRequest = servletPath.contains("/datafeed");
            final boolean isClusterCallRequest = servletPath.contains("clustercall.rpc");

            if (isApiRequest) {
                if (!config.isAuthenticationRequired()) {
                    bypassAuthentication(request, response, chain, false);
                } else {
                    // Authenticate requests to the API.
                    final UserRef userRef = loginAPI(request, response);

                    final String sessionId = Optional.ofNullable(request.getSession(false))
                            .map(HttpSession::getId)
                            .orElse(null);

                    final UserToken userToken = UserTokenUtil.create(userRef.getName(), sessionId);

                    continueAsUser(request, response, chain, userToken, userRef);
                }

            } else if (isClusterCallRequest | isDatafeedRequest) {
                bypassAuthentication(request, response, chain, false);
            } else {
                // Authenticate requests from the UI.

                // Try and get an existing user ref from the session.
                final UserRef userRef = UserRefSessionUtil.get(request.getSession(false));
                if (userRef != null) {
                    final String sessionId = Optional.ofNullable(request.getSession(false))
                            .map(HttpSession::getId)
                            .orElse(null);

                    final UserToken userToken = UserTokenUtil.create(userRef.getName(), sessionId);

                    continueAsUser(request, response, chain, userToken, userRef);

                } else if (!config.isAuthenticationRequired()) {
                    bypassAuthentication(request, response, chain, true);

                } else {
                    // If the session doesn't have a user ref then attempt login.
                    final boolean loggedIn = loginUI(request, response);

                    // If we're not logged in we need to start an AuthenticationRequest flow.
                    if (!loggedIn) {
                        // We were unable to login so we're going to redirect with an AuthenticationRequest.
                        redirectToAuthService(request, response);
                    }
                }
            }
        }
    }

    private void bypassAuthentication(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain, final boolean useSession) throws IOException, ServletException {
        final AuthenticationToken token = new AuthenticationToken("admin", null);
        final UserRef userRef = security.asProcessingUserResult(() -> authenticationService.getUserRef(token));
        if (userRef != null) {
            HttpSession session;
            if (useSession) {
                session = request.getSession(true);

                // Set the user ref in the session.
                UserRefSessionUtil.set(session, userRef);

            } else {
                session = request.getSession(false);
            }

            final String sessionId = Optional.ofNullable(session)
                    .map(HttpSession::getId)
                    .orElse(null);

            final UserToken userToken = UserTokenUtil.create(userRef.getName(), sessionId);
            continueAsUser(request, response, chain, userToken, userRef);
        }
    }

    private void continueAsUser(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final FilterChain chain,
                                final UserToken userToken,
                                final UserRef userRef)
            throws IOException, ServletException {
        if (userRef != null) {
            // If the session already has a reference to a user then continue the chain as that user.
            try {
                CurrentUserState.push(userToken, userRef);

                chain.doFilter(request, response);
            } finally {
                CurrentUserState.pop();
            }
        }
    }

    private boolean ignoreUri(final String uri) {
        return pattern != null && pattern.matcher(uri).matches();
    }

    private boolean loginUI(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        boolean loggedIn = false;

        // If we have a state id then this should be a return from the auth service.
        final String stateId = request.getParameter("state");
        if (stateId != null) {
            LOGGER.debug("We have the following state: {{}}", stateId);

            // Check the state is one we requested.
            final AuthenticationState state = AuthenticationStateSessionUtil.pop(request);
            if (state == null) {
                LOGGER.warn("Unexpected state: " + stateId);

            } else {
                // If we have an access code we can try and log in.
                final String accessCode = request.getParameter("accessCode");
                if (accessCode != null) {
                    LOGGER.debug("We have the following access code: {{}}", accessCode);
                    final AuthenticationToken token = createUIToken(request, state, accessCode);
                    final UserRef userRef = security.asProcessingUserResult(() -> authenticationService.getUserRef(token));

                    if (userRef != null) {
                        // Set the user ref in the session.
                        UserRefSessionUtil.set(request.getSession(true), userRef);

                        loggedIn = true;
                    }

                    // If we manage to login then redirect to the original URL held in the state.
                    if (loggedIn) {
                        LOGGER.info("Redirecting to initiating URL: {}", state.getUrl());
                        response.sendRedirect(state.getUrl());
                    }
                }
            }
        }

        return loggedIn;
    }

    private void redirectToAuthService(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        // Invalidate the current session.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // We have a a new request so we're going to redirect with an AuthenticationRequest.
        final String authenticationRequestBaseUrl = config.getAuthenticationServiceUrl() + "/authenticate";

        // Get the redirect URL for the auth service from the current request.
        String url = request.getRequestURL().toString();
        String query = request.getQueryString();
        if (!Strings.isNullOrEmpty(query)) {
            url += "?" + query;
        }

        // Create a state for this authentication request.
        final AuthenticationState state = AuthenticationStateSessionUtil.create(request, url);

        // If we're using the request URL we want to trim off any trailing params
        final URI parsedRequestUrl = UriBuilder.fromUri(url).build();
        String redirectUrl;

        if (uiConfig.getUrlConfig() != null && uiConfig.getUrlConfig().getUi() != null && uiConfig.getUrlConfig().getUi().trim().length() > 0) {
            LOGGER.debug("Using the advertised URL as the OpenID redirect URL");
            redirectUrl = uiConfig.getUrlConfig().getUi();
        } else {
            redirectUrl = parsedRequestUrl.getScheme() + "://" + parsedRequestUrl.getHost() + ":" + parsedRequestUrl.getPort();
        }

        // Putting the path and query back on
        if (parsedRequestUrl.getPath() != null && parsedRequestUrl.getPath().length() > 0 && !parsedRequestUrl.getPath().equals("/")) {
            redirectUrl += parsedRequestUrl.getPath();
            redirectUrl += "?";
            if (!Strings.isNullOrEmpty(query)) {
                redirectUrl += query;
            }
        }

        // In some cases we might need to use an external URL as the current incoming one might have been proxied.


        String authenticationRequestParams = "" +
                "?scope=openid" +
                "&response_type=code" +
                "&client_id=stroom" +
                "&redirect_url=" +
                URLEncoder.encode(redirectUrl, StreamUtil.DEFAULT_CHARSET_NAME) +
                "&state=" +
                URLEncoder.encode(state.getId(), StreamUtil.DEFAULT_CHARSET_NAME) +
                "&nonce=" +
                URLEncoder.encode(state.getNonce(), StreamUtil.DEFAULT_CHARSET_NAME);

        // If there's 'prompt' in the request then we'll want to pass that on to the AuthenticationService.
        // In OpenId 'prompt=login' asks the IP to present a login page to the user, and that's the effect
        // this will have. We need this so that we can bypass certificate logins, e.g. for when we need to
        // log in as the 'admin' user but the browser is always presenting a certificate.
        String prompt = request.getParameter("prompt");
        if (!Strings.isNullOrEmpty(prompt)) {
            authenticationRequestParams += "&prompt=" + prompt;
        }

        final String authenticationRequestUrl = authenticationRequestBaseUrl + authenticationRequestParams;
        LOGGER.info("Redirecting with an AuthenticationRequest to: {}", authenticationRequestUrl);
        // We want to make sure that the client has the cookie.
        response.sendRedirect(authenticationRequestUrl);
    }

    /**
     * This method must create the token.
     * It does this by enacting the OpenId exchange of accessCode for idToken.
     */
    private AuthenticationToken createUIToken(final HttpServletRequest request, final AuthenticationState state, final String accessCode) {
        AuthenticationToken token = null;

        try {
            String sessionId = request.getSession().getId();
            final String idToken = authenticationServiceClients.newAuthenticationApi().getIdToken(accessCode);
            final JwtClaims jwtClaimsOptional = jwtService.verifyToken(idToken);
            final String nonce = (String) jwtClaimsOptional.getClaimsMap().get("nonce");
            final boolean match = nonce.equals(state.getNonce());
            if (match) {
                LOGGER.info("User is authenticated for sessionId " + sessionId);
                token = new AuthenticationToken(jwtClaimsOptional.getSubject(), idToken);

            } else {
                // If the nonces don't match we need to redirect to log in again.
                // Maybe the request uses an out-of-date stroomSessionId?
                LOGGER.info("Received a bad nonce!");
            }
        } catch (ApiException e) {
            if (e.getCode() == Response.Status.UNAUTHORIZED.getStatusCode()) {
                // If we can't exchange the accessCode for an idToken then this probably means the
                // accessCode doesn't exist any more, or has already been used. so we can't proceed.
                LOGGER.error("The accessCode used to obtain an idToken was rejected. Has it already been used?");
            } else {
                LOGGER.error("Unable to retrieve idToken!", e);
            }
        } catch (final MalformedClaimException | InvalidJwtException e) {
            LOGGER.warn(e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }

        return token;
    }

    private UserRef loginAPI(final HttpServletRequest request, final HttpServletResponse response) {
        UserRef userRef = null;

        // Authenticate requests from an API client
        boolean isAuthenticatedApiRequest = jwtService.containsValidJws(request);
        if (isAuthenticatedApiRequest) {
            final AuthenticationToken token = createAPIToken(request);
            userRef = security.asProcessingUserResult(() -> authenticationService.getUserRef(token));
        }

        if (userRef == null) {
            LOGGER.debug("API request is unauthorised.");
            response.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
        }

        return userRef;
    }

    /**
     * This method creates a token for the API auth flow.
     */
    private AuthenticationToken createAPIToken(final HttpServletRequest request) {
        AuthenticationToken token = null;

        try {
            if (jwtService.containsValidJws(request)) {
                final Optional<String> optionalJws = jwtService.getJws(request);
                final String jws = optionalJws.orElseThrow(() -> new AuthenticationException("Unable to get JWS"));
                final JwtClaims jwtClaims = jwtService.verifyToken(jws);
                token = new AuthenticationToken(jwtClaims.getSubject(), optionalJws.get());
            } else {
                LOGGER.error("Cannot get a valid JWS for API request!");
            }
        } catch (final MalformedClaimException | InvalidJwtException e) {
            LOGGER.warn(e.getMessage());
            throw new AuthenticationException(e.getMessage(), e);
        }

        return token;
    }

    @Override
    public void destroy() {
    }
}
