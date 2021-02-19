/*
 *
 *   Copyright 2017 Crown Copyright
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package stroom.security.identity.authenticate;

import stroom.security.identity.config.PasswordPolicyConfig;
import stroom.security.identity.exceptions.NoSuchUserException;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.Response.Status;

// TODO : @66 Add audit logging
class AuthenticationResourceImpl implements AuthenticationResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResourceImpl.class);

    private final Provider<AuthenticationServiceImpl> serviceProvider;

    @Inject
    AuthenticationResourceImpl(final Provider<AuthenticationServiceImpl> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Timed
    @Override
    public AuthenticationState getAuthenticationState(final HttpServletRequest request) {
        return serviceProvider.get().getAuthenticationState(request);
    }

    @Timed
    @Override
    public LoginResponse login(final HttpServletRequest request,
                               final LoginRequest loginRequest) {
        LOGGER.debug("Received a login request");
        return serviceProvider.get().handleLogin(loginRequest, request);
    }

    @Timed
    @Override
    public Boolean logout(
            final HttpServletRequest request,
            final String redirectUri) {
        LOGGER.debug("Received a logout request");
        final String postLogoutUrl = serviceProvider.get().logout(request, redirectUri);

        try {
            throw new RedirectionException(Status.SEE_OTHER, new URI(postLogoutUrl));
        } catch (final URISyntaxException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Timed
    @Override
    public ConfirmPasswordResponse confirmPassword(final HttpServletRequest request,
                                                   final ConfirmPasswordRequest confirmPasswordRequest) {
        return serviceProvider.get().confirmPassword(request, confirmPasswordRequest);
    }

    @Timed
    @Override
    public final ChangePasswordResponse changePassword(final HttpServletRequest request,
                                                       final ChangePasswordRequest changePasswordRequest) {
        return serviceProvider.get().changePassword(request, changePasswordRequest);
    }

    @Timed
    @Override
    public Boolean resetEmail(
            final HttpServletRequest request,
            final String emailAddress) throws NoSuchUserException {
        final boolean resetEmailSent = serviceProvider.get().resetEmail(emailAddress);
        if (resetEmailSent) {
            return true;
        }

        throw new NotFoundException("User does not exist");
    }

    @Timed
    @Override
    public final ChangePasswordResponse resetPassword(final HttpServletRequest request,
                                                      final ResetPasswordRequest resetPasswordRequest) {
        final ChangePasswordResponse changePasswordResponse = serviceProvider.get().resetPassword(request,
                resetPasswordRequest);
        if (changePasswordResponse != null) {
            return changePasswordResponse;
        }
        throw new NotAuthorizedException("Not authorised");
    }

    @Timed
    @Override
    public final Boolean needsPasswordChange(final String email) {
        return serviceProvider.get().needsPasswordChange(email);
    }

    @Timed
    @Override
    public PasswordPolicyConfig fetchPasswordPolicy() {
        return serviceProvider.get().getPasswordPolicy();
    }
}
