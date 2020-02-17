/*
 * Copyright 2016 Crown Copyright
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

package stroom.config.global.shared;

import stroom.util.shared.BaseCriteria;
import stroom.util.shared.StringCriteria;

public class FindGlobalConfigCriteria extends BaseCriteria {
    private StringCriteria name = new StringCriteria();

    public FindGlobalConfigCriteria() {
        // Default constructor necessary for GWT serialisation.
    }

    public static FindGlobalConfigCriteria create(final String name) {
        FindGlobalConfigCriteria criteria = new FindGlobalConfigCriteria();
        criteria.setName(new StringCriteria(name, null));
        return criteria;
    }

    public StringCriteria getName() {
        return name;
    }

    public void setName(final StringCriteria name) {
        this.name = name;
    }
}
