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
 *
 */

package stroom.ruleset.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import stroom.datasource.api.v2.DataSourceField;
import stroom.docref.SharedObject;
import stroom.docstore.shared.Doc;

import java.util.List;

@JsonPropertyOrder({"type", "uuid", "name", "version", "createTime", "updateTime", "createUser", "updateUser", "fields", "rules"})
@JsonInclude(Include.NON_EMPTY)
public class ReceiveDataRuleSet extends Doc implements SharedObject {
    private static final long serialVersionUID = -7268301402378907741L;

    public static final String DOCUMENT_TYPE = "RuleSet";

    private List<DataSourceField> fields;
    private List<ReceiveDataRule> rules;

    public ReceiveDataRuleSet() {
        // Default constructor for GWT serialisation.
    }

    public List<DataSourceField> getFields() {
        return fields;
    }

    public void setFields(final List<DataSourceField> fields) {
        this.fields = fields;
    }

    public List<ReceiveDataRule> getRules() {
        return rules;
    }

    public void setRules(final List<ReceiveDataRule> rules) {
        this.rules = rules;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final ReceiveDataRuleSet ruleSet = (ReceiveDataRuleSet) o;

        if (fields != null ? !fields.equals(ruleSet.fields) : ruleSet.fields != null) return false;
        return rules != null ? rules.equals(ruleSet.rules) : ruleSet.rules == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        result = 31 * result + (rules != null ? rules.hashCode() : 0);
        return result;
    }
}
