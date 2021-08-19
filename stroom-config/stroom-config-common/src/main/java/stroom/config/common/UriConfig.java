package stroom.config.common;

import stroom.util.config.annotations.ReadOnly;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.IsStroomConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Objects;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

public abstract class UriConfig extends AbstractConfig implements IsStroomConfig {

    private static final String PROP_NAME_SCHEME = "scheme";
    private static final String PROP_NAME_HOSTNAME = "hostname";
    private static final String PROP_NAME_PORT = "port";
    private static final String PROP_NAME_PATH_PREFIX = "pathPrefix";

    private String scheme;
    private String hostname;
    private Integer port;
    private String pathPrefix;

    public UriConfig() {
    }

    public UriConfig(final String scheme, final String hostname, final Integer port) {
        this.scheme = scheme;
        this.hostname = hostname;
        this.port = port;
    }

    public UriConfig(final String scheme, final String hostname) {
        this.scheme = scheme;
        this.hostname = hostname;
    }

    @Pattern(regexp = "^https?$")
    public String getScheme() {
        return scheme;
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    @ReadOnly
    @JsonProperty(PROP_NAME_HOSTNAME)
    public String getHostname() {
        return hostname;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    @Min(0)
    @Max(65535)
    @JsonProperty(PROP_NAME_PORT)
    public Integer getPort() {
        return port;
    }

    public void setPort(final Integer port) {
        this.port = port;
    }

    @JsonProperty(PROP_NAME_PATH_PREFIX)
    @Pattern(regexp = "/[^/]+")
    @JsonPropertyDescription("Any prefix to be added to the beginning of paths for this UIR. " +
            "This may be needed if there is some form of gateway in front of Stroom that requires different paths.")
    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(final String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme).append("://");
        }

        if (hostname != null) {
            sb.append(hostname);
        }

        if (port != null) {
            sb.append(":").append(port.toString());
        }

        if (pathPrefix != null && !pathPrefix.isBlank()) {
            if (!pathPrefix.startsWith("/")) {
                sb.append("/");
            }
            sb.append(pathPrefix);
        } else {
            sb.append("/");
        }

        return sb.toString();
    }

    @SuppressWarnings("checkstyle:needbraces")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UriConfig)) {
            return false;
        }
        final UriConfig uriConfig = (UriConfig) o;
        return Objects.equals(scheme, uriConfig.scheme) &&
                Objects.equals(hostname, uriConfig.hostname) &&
                Objects.equals(port, uriConfig.port) &&
                Objects.equals(pathPrefix, uriConfig.pathPrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, hostname, port, pathPrefix);
    }
}
