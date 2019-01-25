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

package stroom.pipeline.xml.converter.ds3;

import stroom.pipeline.xml.converter.ds3.ref.VarFactoryMap;
import stroom.pipeline.xml.converter.ds3.ref.VarMap;

public class VarFactory extends StoreFactory {
    public VarFactory(final NodeFactory parent, final String id) {
        super(parent, id);
    }

    @Override
    void register(final VarFactoryMap varFactoryMap) {
        // Register this node.
        varFactoryMap.register(this);

        // Register child nodes.
        super.register(varFactoryMap);

        final StringBuilder sb = new StringBuilder();
        setAttributes(sb);
    }

    @Override
    public Var newInstance(final VarMap varMap) {
        return new Var(varMap, this);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.VAR;
    }
}
