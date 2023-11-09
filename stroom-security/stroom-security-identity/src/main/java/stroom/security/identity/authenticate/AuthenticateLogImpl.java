package stroom.security.identity.authenticate;

import stroom.event.logging.api.ObjectInfoProvider;
import stroom.event.logging.api.ObjectType;
import stroom.event.logging.api.StroomEventLoggingService;
import stroom.security.api.SecurityContext;

import event.logging.AuthenticateAction;
import event.logging.AuthenticateEventAction;
import event.logging.MultiObject;
import event.logging.OtherObject;
import event.logging.UpdateEventAction;
import event.logging.User;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class AuthenticateLogImpl implements AuthenticateLog {
//    private static final String AUTH_STATE = "AUTH_STATE";
//    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";
//    private static final String ACCOUNT_LOCKED_MESSAGE =
//    "This account is locked. Please contact your administrator";
//    private static final String ACCOUNT_DISABLED_MESSAGE =
//    "This account is disabled. Please contact your administrator";
//    private static final String ACCOUNT_INACTIVE_MESSAGE =
//    "This account is marked as inactive. Please contact your administrator";
//    //    private static final String NO_SESSION_MESSAGE =
//    "You have no session. Please make an AuthenticationRequest to the Authentication Service.";
//    private static final String SUCCESSFUL_LOGIN_MESSAGE = "User logged in successfully.";
//    private static final String FAILED_LOGIN_MESSAGE = "User attempted to log in but failed.";

    private final StroomEventLoggingService eventLoggingService;
    private final Map<ObjectType, Provider<ObjectInfoProvider>> objectInfoProviderMap;
    private final SecurityContext securityContext;

    @Inject
    public AuthenticateLogImpl(final StroomEventLoggingService eventLoggingService,
                               final Map<ObjectType, Provider<ObjectInfoProvider>> objectInfoProviderMap,
                               final SecurityContext securityContext) {
        this.eventLoggingService = eventLoggingService;
        this.objectInfoProviderMap = objectInfoProviderMap;
        this.securityContext = securityContext;
    }

    @Override
    public void login(final CredentialValidationResult result, final Throwable ex) {

//            // Check the credentials
//            switch (loginResult) {
//                case BAD_CREDENTIALS:
//                    eventLoggingService.createAction("Logon", FAILED_LOGIN_MESSAGE);
//
//                case GOOD_CREDENTIALS:
//
//                    eventLoggingService.createAction("Logon", SUCCESSFUL_LOGIN_MESSAGE);
//
//                case USER_DOES_NOT_EXIST:
//                    eventLoggingService.createAction("Logon", SUCCESSFUL_LOGIN_MESSAGE);
//                case LOCKED_BAD_CREDENTIALS:
//
//                    eventLoggingService.createAction("Logon", FAILED_LOGIN_MESSAGE);
//
//                case LOCKED_GOOD_CREDENTIALS:
//                    eventLoggingService.createAction(
//                            "Logon",
//                            "User attempted to log in but failed because the account is locked.");
//
//                case DISABLED_BAD_CREDENTIALS:
//                    // If the credentials are bad we don't want to reveal the status of the account to the user.
//                    eventLoggingService.createAction(
//                            "Logon",
//                            "User attempted to log in but failed because the account is disabled.");
//
//                case DISABLED_GOOD_CREDENTIALS:
//                    eventLoggingService.createAction(
//                            "Logon",
//                            "User attempted to log in but failed because the account is disabled.");
//
//                case INACTIVE_BAD_CREDENTIALS:
//                    // If the credentials are bad we don't want to reveal the status of the account to the user.
//                    eventLoggingService.createAction(
//                            "Logon",
//                            "User attempted to log in but failed because the account is inactive.");
//
//                case INACTIVE_GOOD_CREDENTIALS:
//                    eventLoggingService.createAction(
//                            "Logon",
//                            "User attempted to log in but failed because the account is inactive.");
//
//                default:
//                    String errorMessage = String.format("%s does not support a LoginResult of %s",
//                            this.getClass().getSimpleName(), loginResult.toString());
//                    throw new NotImplementedException(errorMessage);
//            }
//
//            return loginResponse;
    }


    public void logout(final Throwable ex) {
        eventLoggingService.createEvent(
                "Logout",
                "The user has logged out.",
                AuthenticateEventAction.builder()
                        .withUser(User.builder()
                                .withId(securityContext.getUserIdentityForAudit())
                                .build())
                        .withAction(AuthenticateAction.LOGOFF)
                        .build());
    }

    public void resetEmail(final String emailAddress, final Throwable ex) {
        eventLoggingService.createEvent(
                "ResetPassword",
                "User reset their password",
                UpdateEventAction.builder()
                        .withAfter(MultiObject.builder()
                                .addObject(OtherObject.builder()
                                        .withType("Email address")
                                        .withName(emailAddress)
                                        .build())
                                .build())
                        .build());
    }

    public void changePassword(final Throwable ex) {
        eventLoggingService.createEvent(
                "ChangePassword",
                "User reset their password",
                AuthenticateEventAction.builder()
                        .withUser(User.builder()
                                .withId(securityContext.getUserIdentityForAudit())
                                .build())
                        .withAction(AuthenticateAction.RESET_PASSWORD)
                        .build());
    }
}
