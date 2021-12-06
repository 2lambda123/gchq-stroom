/*
 * Copyright 2019 Crown Copyright
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

import stroom.util.shared.AbstractConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;


@JsonPropertyOrder(alphabetic = true)
public class ContentSecurityConfig extends AbstractConfig {

    public static final String PROP_NAME_CONTENT_SECURITY_POLICY = "contentSecurityPolicy";

    private final String contentSecurityPolicy;
    private final String contentTypeOptions;
    private final String frameOptions;
    private final String xssProtection;

    public ContentSecurityConfig() {
        contentSecurityPolicy = "" +
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-eval' 'unsafe-inline'; " +
                "img-src 'self' data:; " +
                "style-src 'self' 'unsafe-inline'; " +
                "frame-ancestors 'self';";
        contentTypeOptions = "nosniff";
        frameOptions = "sameorigin";
        xssProtection = "1; mode=block";
    }

    @JsonCreator
    public ContentSecurityConfig(@JsonProperty(PROP_NAME_CONTENT_SECURITY_POLICY) final String contentSecurityPolicy,
                                 @JsonProperty("contentTypeOptions") final String contentTypeOptions,
                                 @JsonProperty("frameOptions") final String frameOptions,
                                 @JsonProperty("xssProtection") final String xssProtection) {
        this.contentSecurityPolicy = contentSecurityPolicy;
        this.contentTypeOptions = contentTypeOptions;
        this.frameOptions = frameOptions;
        this.xssProtection = xssProtection;
    }

    @JsonProperty(PROP_NAME_CONTENT_SECURITY_POLICY)
    @JsonPropertyDescription("The content security policy")
    public String getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    @JsonPropertyDescription("The content type options")
    public String getContentTypeOptions() {
        return contentTypeOptions;
    }

    @JsonPropertyDescription("The frame options")
    public String getFrameOptions() {
        return frameOptions;
    }

    @JsonPropertyDescription("XSS protection")
    public String getXssProtection() {
        return xssProtection;
    }

    public ContentSecurityConfig withContentSecurityPolicy(final String contentSecurityPolicy) {
        return new ContentSecurityConfig(contentSecurityPolicy, contentTypeOptions, frameOptions, xssProtection);
    }

    @Override
    public String toString() {
        return "ContentSecurityConfig{" +
                "contentSecurityPolicy='" + contentSecurityPolicy + '\'' +
                ", contentTypeOptions='" + contentTypeOptions + '\'' +
                ", frameOptions='" + frameOptions + '\'' +
                ", xssProtection='" + xssProtection + '\'' +
                '}';
    }
}
