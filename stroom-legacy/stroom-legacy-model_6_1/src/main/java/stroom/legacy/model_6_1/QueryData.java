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

package stroom.legacy.model_6_1;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "query", propOrder = {"dataSource", "expression", "limits"})
@XmlRootElement(name = "query")
@Deprecated
public class QueryData implements Serializable {
    private static final long serialVersionUID = -2530827581046882396L;

    private DocRef dataSource;
    private ExpressionOperator expression;
    private Limits limits;

    public QueryData() {
        // Default constructor necessary for GWT serialisation.
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
