package stroom.security.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import stroom.util.config.annotations.ReadOnly;
import stroom.util.config.annotations.RequiresRestart;
import stroom.util.shared.IsConfig;

import javax.inject.Singleton;

@Singleton
public class AuthenticationConfig implements IsConfig {
    private String authenticationServiceUrl;
    private boolean authenticationRequired = true;
    private boolean verifySsl;
    private String apiToken = "eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE1Mzg2NDM1NTQsInN1YiI6ImFkbWluIiwiaXNzIjoic3Ryb29tIn0.J8dqtQf9gGXQlKU_rAye46lUKlJR8-vcyrYhOD0Rxoc";
    private String authServicesBaseUrl = "http://auth-service:8099";
    private String durationToWarnBeforeExpiry = "30d";
    private JwtConfig jwtConfig = new JwtConfig();
    private boolean preventLogin;
    private String userNamePattern = "^[a-zA-Z0-9_-]{3,}$";

    @JsonPropertyDescription("The URL of the authentication service")
    public String getAuthenticationServiceUrl() {
        return authenticationServiceUrl;
    }

    public void setAuthenticationServiceUrl(final String authenticationServiceUrl) {
        this.authenticationServiceUrl = authenticationServiceUrl;
    }

    @ReadOnly
    @JsonPropertyDescription("Choose whether Stroom requires authenticated access")
    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    public void setAuthenticationRequired(final boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    @ReadOnly
    @JsonPropertyDescription("If using HTTPS should we verify the server certs")
    public boolean isVerifySsl() {
        return verifySsl;
    }

    @ReadOnly
    @JsonPropertyDescription("If using HTTPS should we verify the server certs")
    public void setVerifySsl(final boolean verifySsl) {
        this.verifySsl = verifySsl;
    }

    @RequiresRestart(RequiresRestart.RestartScope.UI)
    @JsonPropertyDescription("The API token Stroom will use to authenticate itself when accessing other services")
    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(final String apiToken) {
        this.apiToken = apiToken;
    }

    @JsonPropertyDescription("The URL of the auth service")
    public String getAuthServicesBaseUrl() {
        return authServicesBaseUrl;
    }

    public void setAuthServicesBaseUrl(final String authServicesBaseUrl) {
        this.authServicesBaseUrl = authServicesBaseUrl;
    }

    @JsonProperty("durationToWarnBeforeExpiry")
    public String getDurationToWarnBeforeExpiry() {
        return durationToWarnBeforeExpiry;
    }

    public void setDurationToWarnBeforeExpiry(final String durationToWarnBeforeExpiry) {
        this.durationToWarnBeforeExpiry = durationToWarnBeforeExpiry;
    }

    @JsonProperty("jwt")
    public JwtConfig getJwtConfig() {
        return jwtConfig;
    }

    public  void setJwtConfig(final JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @JsonPropertyDescription("Prevent new logins to the system. This is useful if the system is scheduled to " +
            "have an outage.")
    public boolean isPreventLogin() {
        return preventLogin;
    }

    public void setPreventLogin(final boolean preventLogin) {
        this.preventLogin = preventLogin;
    }

    @JsonPropertyDescription("The regex pattern for user names")
    public String getUserNamePattern() {
        return userNamePattern;
    }

    public void setUserNamePattern(final String userNamePattern) {
        this.userNamePattern = userNamePattern;
    }

    @Override
    public String toString() {
        return "AuthenticationConfig{" +
                "authenticationServiceUrl='" + authenticationServiceUrl + '\'' +
                ", authenticationRequired=" + authenticationRequired +
                ", apiToken='" + apiToken + '\'' +
                ", authServicesBaseUrl='" + authServicesBaseUrl + '\'' +
                ", durationToWarnBeforeExpiry='" + durationToWarnBeforeExpiry + '\'' +
                ", preventLogin=" + preventLogin +
                ", userNamePattern='" + userNamePattern + '\'' +
                '}';
    }

    public static class JwtConfig implements IsConfig {
        private String jwtIssuer= "stroom";
        private boolean enableTokenRevocationCheck = true;

        @RequiresRestart(RequiresRestart.RestartScope.UI)
        @JsonPropertyDescription("The issuer to expect when verifying JWTs.")
        public String getJwtIssuer() {
            return jwtIssuer;
        }

        public void setJwtIssuer(final String jwtIssuer) {
            this.jwtIssuer = jwtIssuer;
        }

        @RequiresRestart(RequiresRestart.RestartScope.UI)
        @JsonPropertyDescription("Whether or not to enable remote calls to the auth service to check if " +
                "a token we have has been revoked.")
        public boolean isEnableTokenRevocationCheck() {
            return enableTokenRevocationCheck;
        }

        public void setEnableTokenRevocationCheck(final boolean enableTokenRevocationCheck) {
            this.enableTokenRevocationCheck = enableTokenRevocationCheck;
        }

        @Override
        public String toString() {
            return "JwtConfig{" +
                    "jwtIssuer='" + jwtIssuer + '\'' +
                    ", enableTokenRevocationCheck=" + enableTokenRevocationCheck +
                    '}';
        }
    }
}
