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

package stroom.security.identity.config;

import stroom.config.common.DbConfig;
import stroom.config.common.HasDbConfig;
import stroom.util.config.annotations.ReadOnly;
import stroom.util.config.annotations.RequiresRestart;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.validation.ValidRegex;
import stroom.util.shared.validation.ValidationSeverity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Singleton
@JsonPropertyOrder(alphabetic = true)
public final class IdentityConfig extends AbstractConfig implements HasDbConfig {

    public static final String PROP_NAME_EMAIL = "email";
    public static final String PROP_NAME_TOKEN = "token";
    public static final String PROP_NAME_OPENID = "openid";
    public static final String PROP_NAME_PASSWORD_POLICY = "passwordPolicy";

    private boolean useDefaultOpenIdCredentials;
    private boolean allowCertificateAuthentication;
    private String certificateCnPattern = ".*\\((.*)\\)";
    private int certificateCnCaptureGroupIndex = 1;
    private Integer failedLoginLockThreshold = 3;

    private EmailConfig emailConfig = new EmailConfig();
    private TokenConfig tokenConfig = new TokenConfig();
    private OpenIdConfig openIdConfig = new OpenIdConfig();
    private PasswordPolicyConfig passwordPolicyConfig = new PasswordPolicyConfig();
    private DbConfig dbConfig = new DbConfig();

    @AssertFalse(
            message = "Using default OpenId authentication credentials. These should only be used " +
                    "in test/demo environments. Set stroom.authentication.useDefaultOpenIdCredentials to false for " +
                    "production environments.",
            payload = ValidationSeverity.Warning.class)
    @RequiresRestart(RequiresRestart.RestartScope.SYSTEM)
    @ReadOnly
    @JsonProperty()
    @JsonPropertyDescription("If true, stroom will use a set of default authentication credentials to allow" +
            "API calls from stroom-proxy. For test or demonstration purposes only, set to false for production")
    public boolean isUseDefaultOpenIdCredentials() {
        return useDefaultOpenIdCredentials;
    }

    @SuppressWarnings("unused")
    public void setUseDefaultOpenIdCredentials(final boolean useDefaultOpenIdCredentials) {
        this.useDefaultOpenIdCredentials = useDefaultOpenIdCredentials;
    }

    @RequiresRestart(RequiresRestart.RestartScope.SYSTEM)
    @JsonProperty
    @JsonPropertyDescription("In order for clients to be able to login with certificates this property must be set " +
            "to true. For security certificate authentication should not be allowed unless the application is " +
            "adequately secured and HTTPS is configured either directly for DropWizard or by an appropriate reverse " +
            "proxy such as NGINX.")
    public boolean isAllowCertificateAuthentication() {
        return allowCertificateAuthentication;
    }

    public void setAllowCertificateAuthentication(final boolean allowCertificateAuthentication) {
        this.allowCertificateAuthentication = allowCertificateAuthentication;
    }

    @NotNull
    @ValidRegex
    @JsonProperty
    @JsonPropertyDescription("The regular expression pattern that represents the Common Name (CN) value in an X509 " +
            "certificate. The pattern should include a capture group for extracting the user identity from the " +
            "CN value. For example the CN may be of the form 'Joe Bloggs [jbloggs]' in which case the pattern " +
            "would be '.*?\\[([^]]*)\\].*'. The only capture group surrounds the user identity part.")
    public final String getCertificateCnPattern() {
        return this.certificateCnPattern;
    }

    @SuppressWarnings("unused")
    public void setCertificateCnPattern(final String certificateCnPattern) {
        this.certificateCnPattern = certificateCnPattern;
    }

    @NotNull
    @Min(0)
    @JsonProperty
    @JsonPropertyDescription("Used in conjunction with property certificateCnPattern. This property value is the " +
            "number of the regex capture group that represents the portion of certificate Common Name (CN) value " +
            "that is the user identity. If all of the CN value is the user identity then set this property to " +
            "0 to capture the whole CN value.")
    public int getCertificateCnCaptureGroupIndex() {
        return certificateCnCaptureGroupIndex;
    }

    @SuppressWarnings("unused")
    public void setCertificateCnCaptureGroupIndex(final int certificateCnCaptureGroupIndex) {
        this.certificateCnCaptureGroupIndex = certificateCnCaptureGroupIndex;
    }

    @Nullable
    @JsonProperty(PROP_NAME_EMAIL)
    public EmailConfig getEmailConfig() {
        return emailConfig;
    }

    @SuppressWarnings("unused")
    public void setEmailConfig(EmailConfig emailConfig) {
        this.emailConfig = emailConfig;
    }

    @Nullable
    @JsonProperty
    @JsonPropertyDescription("If the number of failed logins is greater than or equal to this value then the " +
            "account  will be locked.")
    @Min(1)
    public Integer getFailedLoginLockThreshold() {
        return this.failedLoginLockThreshold;
    }

    @SuppressWarnings("unused")
    public void setFailedLoginLockThreshold(final Integer failedLoginLockThreshold) {
        this.failedLoginLockThreshold = failedLoginLockThreshold;
    }

    @NotNull
    @JsonProperty(PROP_NAME_TOKEN)
    public TokenConfig getTokenConfig() {
        return tokenConfig;
    }

    @SuppressWarnings("unused")
    public void setTokenConfig(TokenConfig tokenConfig) {
        this.tokenConfig = tokenConfig;
    }

    @NotNull
    @JsonProperty(PROP_NAME_OPENID)
    public OpenIdConfig getOpenIdConfig() {
        return openIdConfig;
    }

    @SuppressWarnings("unused")
    public void setOpenIdConfig(final OpenIdConfig openIdConfig) {
        this.openIdConfig = openIdConfig;
    }

    @NotNull
    @JsonProperty(PROP_NAME_PASSWORD_POLICY)
    public PasswordPolicyConfig getPasswordPolicyConfig() {
        return passwordPolicyConfig;
    }

    @SuppressWarnings("unused")
    public void setPasswordPolicyConfig(PasswordPolicyConfig passwordPolicyConfig) {
        this.passwordPolicyConfig = passwordPolicyConfig;
    }

    @JsonProperty("db")
    public DbConfig getDbConfig() {
        return dbConfig;
    }

    public void setDbConfig(final DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }
}
