package stroom.security.identity.config;

import stroom.util.shared.AbstractConfig;
import stroom.util.shared.validation.ValidRegex;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;
import javax.inject.Singleton;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Singleton
@JsonPropertyOrder(alphabetic = true)
@JsonInclude(Include.NON_NULL)
public class PasswordPolicyConfig extends AbstractConfig {

    private static final Boolean ALLOW_PASSWORD_RESETS_DEFAULT = Boolean.TRUE;
    private static final Boolean FORCE_PASSWORD_CHANGE_ON_FIRST_LOGIN = Boolean.TRUE;

    @JsonProperty
    @JsonPropertyDescription("Will the UI allow password resets")
    private Boolean allowPasswordResets;

    @NotNull
    @JsonProperty
    @JsonPropertyDescription("Unused user accounts with a duration since account creation greater than this " +
            "value will be locked. The frequency of checks is " +
            "controlled by the job 'Account Maintenance'.")
    private StroomDuration neverUsedAccountDeactivationThreshold;

    @NotNull
    @JsonProperty
    @JsonPropertyDescription("User accounts with a duration since last login greater than this " +
            "value will be locked. The frequency of checks is " +
            "controlled by the job 'Account Maintenance'.")
    private StroomDuration unusedAccountDeactivationThreshold;

    @NotNull
    @JsonProperty
    @JsonPropertyDescription("The age after which a password will have to be changed. The frequency of checks is " +
            "controlled by the job 'Account Maintenance'.")
    private StroomDuration mandatoryPasswordChangeDuration;

    @JsonProperty
    @JsonPropertyDescription("If true, on first login the user will be forced to change their password.")
    private Boolean forcePasswordChangeOnFirstLogin;

    @ValidRegex
    @JsonProperty
    @JsonPropertyDescription("A regex pattern that new passwords must match")
    // The default is to let everything through
    private String passwordComplexityRegex;

    @Min(0)
    @Max(5)
    @NotNull
    @JsonProperty
    @JsonPropertyDescription("The minimum strength password that is allowed.")
    private Integer minimumPasswordStrength;

    @Min(0)
    @NotNull
    @JsonProperty
    @JsonPropertyDescription("The minimum number of characters that new passwords need to contain.")
    private Integer minimumPasswordLength;

    @JsonProperty
    @JsonPropertyDescription("A message informing users of the password policy")
    private String passwordPolicyMessage;

    public PasswordPolicyConfig() {
        setDefaults();
    }

    @JsonCreator
    public PasswordPolicyConfig(
            @JsonProperty("allowPasswordResets") final Boolean allowPasswordResets,
            @JsonProperty("neverUsedAccountDeactivationThreshold") final
            StroomDuration neverUsedAccountDeactivationThreshold,
            @JsonProperty("unusedAccountDeactivationThreshold") final StroomDuration unusedAccountDeactivationThreshold,
            @JsonProperty("mandatoryPasswordChangeDuration") final StroomDuration mandatoryPasswordChangeDuration,
            @JsonProperty("forcePasswordChangeOnFirstLogin") final Boolean forcePasswordChangeOnFirstLogin,
            @JsonProperty("passwordComplexityRegex") final String passwordComplexityRegex,
            @JsonProperty("minimumPasswordStrength") final Integer minimumPasswordStrength,
            @JsonProperty("minimumPasswordLength") final Integer minimumPasswordLength,
            @JsonProperty("passwordPolicyMessage") final String passwordPolicyMessage) {

        this.allowPasswordResets = allowPasswordResets;
        this.neverUsedAccountDeactivationThreshold = neverUsedAccountDeactivationThreshold;
        this.unusedAccountDeactivationThreshold = unusedAccountDeactivationThreshold;
        this.mandatoryPasswordChangeDuration = mandatoryPasswordChangeDuration;
        this.forcePasswordChangeOnFirstLogin = forcePasswordChangeOnFirstLogin;
        this.passwordComplexityRegex = passwordComplexityRegex;
        this.minimumPasswordStrength = minimumPasswordStrength;
        this.minimumPasswordLength = minimumPasswordLength;
        this.passwordPolicyMessage = passwordPolicyMessage;

        setDefaults();
    }

    private void setDefaults() {
        if (allowPasswordResets == null) {
            allowPasswordResets = ALLOW_PASSWORD_RESETS_DEFAULT;
        }
        if (neverUsedAccountDeactivationThreshold == null) {
            neverUsedAccountDeactivationThreshold = StroomDuration.ofDays(30);
        }
        if (unusedAccountDeactivationThreshold == null) {
            unusedAccountDeactivationThreshold = StroomDuration.ofDays(90);
        }
        if (mandatoryPasswordChangeDuration == null) {
            mandatoryPasswordChangeDuration = StroomDuration.ofDays(90);
        }
        if (forcePasswordChangeOnFirstLogin == null) {
            forcePasswordChangeOnFirstLogin = FORCE_PASSWORD_CHANGE_ON_FIRST_LOGIN;
        }
        if (passwordComplexityRegex == null) {
            passwordComplexityRegex = ".*";
        }
        if (minimumPasswordStrength == null) {
            minimumPasswordStrength = 3;
        }
        if (minimumPasswordLength == null) {
            minimumPasswordLength = 8;
        }
        if (passwordPolicyMessage == null) {
            passwordPolicyMessage = "To conform with our Strong Password policy, you are required to use" +
                    " a sufficiently strong password. Password must be more than " +
                    minimumPasswordLength +
                    " characters.";
        }
    }

    public boolean isAllowPasswordResets() {
        return Objects.requireNonNullElse(allowPasswordResets, ALLOW_PASSWORD_RESETS_DEFAULT);
    }

    public void setAllowPasswordResets(final boolean allowPasswordResets) {
        this.allowPasswordResets = Objects.requireNonNullElse(allowPasswordResets, ALLOW_PASSWORD_RESETS_DEFAULT);
    }

    public StroomDuration getNeverUsedAccountDeactivationThreshold() {
        return neverUsedAccountDeactivationThreshold;
    }

    @SuppressWarnings("unused")
    public void setNeverUsedAccountDeactivationThreshold(final StroomDuration neverUsedAccountDeactivationThreshold) {
        this.neverUsedAccountDeactivationThreshold = neverUsedAccountDeactivationThreshold;
    }

    public StroomDuration getUnusedAccountDeactivationThreshold() {
        return unusedAccountDeactivationThreshold;
    }

    @SuppressWarnings("unused")
    public void setUnusedAccountDeactivationThreshold(final StroomDuration unusedAccountDeactivationThreshold) {
        this.unusedAccountDeactivationThreshold = unusedAccountDeactivationThreshold;
    }

    public StroomDuration getMandatoryPasswordChangeDuration() {
        return mandatoryPasswordChangeDuration;
    }

    @SuppressWarnings("unused")
    public void setMandatoryPasswordChangeDuration(final StroomDuration mandatoryPasswordChangeDuration) {
        this.mandatoryPasswordChangeDuration = mandatoryPasswordChangeDuration;
    }

    public boolean isForcePasswordChangeOnFirstLogin() {
        return forcePasswordChangeOnFirstLogin;
    }

    @SuppressWarnings("unused")
    public void setForcePasswordChangeOnFirstLogin(final boolean forcePasswordChangeOnFirstLogin) {
        this.forcePasswordChangeOnFirstLogin = Objects.requireNonNullElse(
                forcePasswordChangeOnFirstLogin, FORCE_PASSWORD_CHANGE_ON_FIRST_LOGIN);
    }

    public String getPasswordComplexityRegex() {
        return passwordComplexityRegex;
    }

    @SuppressWarnings("unused")
    public void setPasswordComplexityRegex(final String passwordComplexityRegex) {
        this.passwordComplexityRegex = passwordComplexityRegex;
    }

    public Integer getMinimumPasswordStrength() {
        return minimumPasswordStrength;
    }

    public void setMinimumPasswordStrength(final Integer minimumPasswordStrength) {
        this.minimumPasswordStrength = minimumPasswordStrength;
    }

    public int getMinimumPasswordLength() {
        return minimumPasswordLength;
    }

    @SuppressWarnings("unused")
    public void setMinimumPasswordLength(final int minimumPasswordLength) {
        this.minimumPasswordLength = minimumPasswordLength;
    }

    public String getPasswordPolicyMessage() {
        return passwordPolicyMessage;
    }

    public void setPasswordPolicyMessage(final String passwordPolicyMessage) {
        this.passwordPolicyMessage = passwordPolicyMessage;
    }
}
