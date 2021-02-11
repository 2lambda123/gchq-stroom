package stroom.security.identity.account;

import stroom.security.identity.authenticate.CredentialValidationResult;

import java.time.Duration;
import java.util.Optional;

public interface AccountDao {
    AccountResultPage list();

    AccountResultPage search(SearchAccountRequest request);

    Account create(Account account, String password);

    Optional<Integer> getId(String userId);

    Optional<Account> get(String userId);

    Optional<Account> get(int id);

    void update(Account account);

    void delete(int id);

    void recordSuccessfulLogin(String userId);

    CredentialValidationResult validateCredentials(String username, String password);

    boolean incrementLoginFailures(String userId);

    void changePassword(String userId, String newPassword);

    Boolean needsPasswordChange(String userId, Duration mandatoryPasswordChangeDuration, boolean forcePasswordChangeOnFirstLogin);

    int deactivateNewInactiveUsers(Duration neverUsedAccountDeactivationThreshold);

    int deactivateInactiveUsers(Duration unusedAccountDeactivationThreshold);
}
