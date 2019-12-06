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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import stroom.docref.HasDisplayValue;
import stroom.util.shared.EqualsBuilder;
import stroom.util.shared.HashCodeBuilder;
import stroom.util.shared.ToStringBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@JsonPropertyOrder({"order", "direction"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "sort")
@XmlType(name = "Sort", propOrder = {"order", "direction"})
public class Sort implements Serializable {
    private static final long serialVersionUID = 4530846367973824427L;

    @XmlElement(name = "order")
    @JsonProperty("order")
    private int order = 1;
    @XmlElement(name = "direction")
    @JsonProperty("direction")
    private SortDirection direction = SortDirection.ASCENDING;

    public Sort() {
        // Default constructor necessary for GWT serialisation.
    }

    public Sort(final int order, final SortDirection direction) {
        this.order = order;
        this.direction = direction;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public SortDirection getDirection() {
        return direction;
    }

    public void setDirection(final SortDirection direction) {
        this.direction = direction;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Sort)) {
            return false;
        }

        final Sort sort = (Sort) o;
        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(order, sort.order);
        builder.append(direction, sort.direction);
        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(order);
        builder.append(direction);
        return builder.toHashCode();
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder();
        builder.append("order", order);
        builder.append("direction", direction);
        return builder.toString();
    }

    public Sort copy() {
        return new Sort(order, direction);
    }

    public enum SortDirection implements HasDisplayValue {
        ASCENDING("Ascending"), DESCENDING("Descending");

        private final String displayValue;

        SortDirection(final String displayValue) {
            this.displayValue = displayValue;
        }

        @Override
        public String getDisplayValue() {
            return displayValue;
        }
    }
}
