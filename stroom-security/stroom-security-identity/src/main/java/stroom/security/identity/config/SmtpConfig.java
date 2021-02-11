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

import stroom.util.shared.AbstractConfig;
import stroom.util.shared.NotInjectableConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.simplejavamail.mailer.config.TransportStrategy;

import javax.validation.constraints.NotNull;

@NotInjectableConfig
public class SmtpConfig extends AbstractConfig {

    @NotNull
    @JsonProperty
    private String host = "localhost";

    @NotNull
    @JsonProperty
    private int port = 2525;

    @NotNull
    @JsonProperty
    private String transport = "plain";

    @NotNull
    @JsonProperty
    private String username = "username";

    @NotNull
    @JsonProperty
    private String password = "password";

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(final String transport) {
        this.transport = transport;
    }

    public TransportStrategy getTransportStrategy() {
        switch (transport) {
            case "TLS":
                return TransportStrategy.SMTP_TLS;
            case "SSL":
                return TransportStrategy.SMTP_TLS;
            case "plain":
                return TransportStrategy.SMTP_PLAIN;
            default:
                return TransportStrategy.SMTP_PLAIN;
        }
    }
}
