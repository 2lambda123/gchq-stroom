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

package stroom.legacy.model_6_1;

import stroom.legacy.model_6_1.ExpressionTerm.Condition;

import java.io.Serializable;

@Deprecated
public class StreamAttributeCondition implements Serializable {
    private static final long serialVersionUID = -2063409357774838870L;

    private StreamAttributeKey streamAttributeKey;
    private Condition condition;
    private String fieldValue;

    public StreamAttributeCondition() {
        // Default constructor necessary for GWT serialisation.
    }

    public StreamAttributeCondition(final StreamAttributeKey streamAttributeKey, final Condition condition,
                                    final String fieldValue) {
        this.streamAttributeKey = streamAttributeKey;
        this.condition = condition;
        this.fieldValue = fieldValue;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(final Condition condition) {
        this.condition = condition;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(final String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public StreamAttributeKey getStreamAttributeKey() {
        return streamAttributeKey;
    }

    public void setStreamAttributeKey(final StreamAttributeKey streamAttributeKey) {
        this.streamAttributeKey = streamAttributeKey;
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(streamAttributeKey);
        builder.append(condition);
        builder.append(fieldValue);
        return builder.toHashCode();
    }

    @SuppressWarnings("checkstyle:needbraces")
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof StreamAttributeCondition)) {
            return false;
        }

        final StreamAttributeCondition streamAttributeCondition = (StreamAttributeCondition) o;
        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(streamAttributeKey, streamAttributeCondition.streamAttributeKey);
        builder.append(condition, streamAttributeCondition.condition);
        builder.append(fieldValue, streamAttributeCondition.fieldValue);

        return builder.isEquals();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StreamAttributeCondition{");
        sb.append("streamAttributeKey=").append(streamAttributeKey);
        sb.append(", condition=").append(condition);
        sb.append(", fieldValue='").append(fieldValue).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
