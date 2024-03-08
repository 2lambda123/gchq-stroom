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

package stroom.search.elastic.shared;

import stroom.docref.HasDisplayValue;

/**
 * @see "https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html"
 */
public enum ElasticIndexFieldType implements HasDisplayValue {
    ID("Id", true),
    BOOLEAN("Boolean", false),
    INTEGER("Integer", true),
    LONG("Long", true),
    FLOAT("Float", false),
    DOUBLE("Double", false),
    DATE("Date", false),
    TEXT("Text", false),
    KEYWORD("Keyword", false),
    IPV4_ADDRESS("IpV4Address", false);

    private final String displayValue;
    private final boolean numeric;

    ElasticIndexFieldType(final String displayValue,
                          final boolean numeric) {
        this.displayValue = displayValue;
        this.numeric = numeric;
    }

    public boolean isNumeric() {
        return numeric;
    }

    @Override
    public String getDisplayValue() {
        return displayValue;
    }
}
