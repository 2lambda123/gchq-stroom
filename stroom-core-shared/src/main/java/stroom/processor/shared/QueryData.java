/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.processor.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionOperator;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlType(name = "query", propOrder = {"dataSource", "expression", "limits"})
@XmlRootElement(name = "query")
@JsonInclude(Include.NON_DEFAULT)
public class QueryData implements Serializable {
    private static final long serialVersionUID = -2530827581046882396L;

    @JsonProperty
    private DocRef dataSource;
    @JsonProperty
    private ExpressionOperator expression;
    @JsonProperty
    private Limits limits;

    public QueryData() {
        // Default constructor necessary for GWT serialisation.
    }

    @JsonCreator
    public QueryData(@JsonProperty("dataSource") final DocRef dataSource,
                     @JsonProperty("expression") final ExpressionOperator expression,
                     @JsonProperty("limits") final Limits limits) {
        this.dataSource = dataSource;
        this.expression = expression;
        this.limits = limits;
    }

    @XmlElement
    public DocRef getDataSource() {
        return dataSource;
    }

    public void setDataSource(final DocRef dataSource) {
        this.dataSource = dataSource;
    }

    @XmlElement
    public ExpressionOperator getExpression() {
        return expression;
    }

    public void setExpression(final ExpressionOperator expression) {
        this.expression = expression;
    }

    @XmlElement
    public Limits getLimits() {
        return limits;
    }

    public void setLimits(final Limits limits) {
        this.limits = limits;
    }

    public static class Builder {

        private final QueryData instance;

        public Builder() {
            this.instance = new QueryData();
        }

        public Builder dataSource(final DocRef value) {
            this.instance.dataSource = value;
            return this;
        }

        public Builder limits(final Limits value) {
            this.instance.limits = value;
            return this;
        }

        public Builder expression(final ExpressionOperator value) {
            this.instance.expression = value;
            return this;
        }

        public QueryData build() {
            return instance;
        }
    }
}
