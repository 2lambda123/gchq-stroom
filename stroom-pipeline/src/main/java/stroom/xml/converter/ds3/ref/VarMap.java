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

package stroom.xml.converter.ds3.ref;

import stroom.xml.converter.ds3.Var;

import java.util.HashMap;
import java.util.Map;

public class VarMap {
    private final Map<String, Var> map = new HashMap<String, Var>();

    /**
     * Register a var node with it's id.
     */
    public void registerVar(final String id, final Var var) {
        map.put(id, var);
    }

    /**
     * Get a var node by id.
     */
    public Var getVar(final String id) {
        return map.get(id);
    }
}
