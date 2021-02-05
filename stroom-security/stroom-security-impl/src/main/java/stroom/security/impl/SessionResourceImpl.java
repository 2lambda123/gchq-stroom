package stroom.security.impl;

import stroom.security.api.UserIdentity;
import stroom.security.impl.session.SessionListResponse;
import stroom.security.impl.session.SessionListService;
import stroom.security.impl.session.UserIdentitySessionUtil;
import stroom.security.openid.api.OpenId;
import stroom.util.rest.RestUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public class SessionResourceImpl implements SessionResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionResourceImpl.class);

    private final AuthenticationEventLog eventLog;
    private final SessionListService sessionListService;
    private final OpenIdManager openIdManager;

    @Inject
    public SessionResourceImpl(final AuthenticationEventLog eventLog,
                               final SessionListService sessionListService,
                               final OpenIdManager openIdManager) {
        this.eventLog = eventLog;
        this.sessionListService = sessionListService;
        this.openIdManager = openIdManager;
    }

    @Override
    public LoginResponse login(final HttpServletRequest request, final String referrer) {
        String redirectUri = null;
        try {
            LOGGER.info("Logging in session for '{}'", referrer);

            final Optional<UserIdentity> userIdentity = openIdManager.loginWithRequestToken(request);
            if (userIdentity.isEmpty()) {
                // If the session doesn't have a user ref then attempt login.
                final Map<String, String> paramMap = UrlUtils.createParamMap(referrer);
                final String code = paramMap.get(OpenId.CODE);
                final String stateId = paramMap.get(OpenId.STATE);
                final String postAuthRedirectUri = OpenId.removeReservedParams(referrer);
                redirectUri = openIdManager.redirect(request, code, stateId, postAuthRedirectUri);
            }

            if (redirectUri == null) {
                redirectUri = OpenId.removeReservedParams(referrer);
            }

            return new LoginResponse(userIdentity.isPresent(), redirectUri);

        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Response logout(final String authSessionId) {
        LOGGER.info("Logging out session {}", authSessionId);

        // TODO : We need to lookup the auth session in our user sessions

        final HttpSession session = SessionMap.getSession(authSessionId);
        final Optional<UserIdentity> userIdentity = UserIdentitySessionUtil.get(session);
        if (session != null) {
            // Invalidate the current user session
            session.invalidate();
        }
        userIdentity.ifPresent(ui -> {
            // Create an event for logout
            eventLog.logoff(ui.getId());
        });

        return RestUtil.ok("Logout successful");
    }

    @Override
    public SessionListResponse list(final String nodeName) {
        LOGGER.debug("list({}) called", nodeName);
        if (nodeName != null) {
            return sessionListService.listSessions(nodeName);
        } else {
            return sessionListService.listSessions();
        }
    }

}
