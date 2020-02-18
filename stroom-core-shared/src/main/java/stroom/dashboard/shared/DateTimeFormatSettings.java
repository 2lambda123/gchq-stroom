/*
 * Copyright 2017 Crown Copyright
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

package stroom.dashboard.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import stroom.util.shared.EqualsBuilder;
import stroom.util.shared.HashCodeBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@JsonPropertyOrder({"pattern", "timeZone"})
@JsonInclude(Include.NON_DEFAULT)
@XmlRootElement(name = "dateTimeFormatSettings")
@XmlType(name = "DateTimeFormatSettings", propOrder = {"pattern", "timeZone"})
public class DateTimeFormatSettings implements FormatSettings {
    private static final long serialVersionUID = 9145624653060319801L;

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";

    @XmlElement(name = "pattern")
    @JsonProperty("pattern")
    private String pattern;
    @XmlElement(name = "timeZone")
    @JsonProperty("timeZone")
    private TimeZone timeZone;

    public DateTimeFormatSettings() {
    }

    @JsonCreator
    public DateTimeFormatSettings(@JsonProperty("pattern") final String pattern,
                                  @JsonProperty("timeZone") final TimeZone timeZone) {
        this.pattern = pattern;
        this.timeZone = timeZone;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(final TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    @XmlTransient
    @Override
    public boolean isDefault() {
        return pattern == null || pattern.equals(DEFAULT_PATTERN);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        DateTimeFormatSettings that = (DateTimeFormatSettings) o;

        return new EqualsBuilder()
                .append(pattern, that.pattern)
                .append(timeZone, that.timeZone)
                .isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(pattern);
        hashCodeBuilder.append(timeZone);
        return hashCodeBuilder.toHashCode();
    }

    @Override
    public String toString() {
        return "DateTimeFormatSettings{" +
                "pattern='" + pattern + '\'' +
                ", timeZone=" + timeZone +
                '}';
    }

    @Override
    public FormatSettings copy() {
        return new DateTimeFormatSettings(pattern, timeZone);
    }
}
