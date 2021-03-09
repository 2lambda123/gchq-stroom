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

package stroom.pipeline.filter;

import stroom.pipeline.xml.event.EventListUtils;

import net.sf.saxon.om.NodeInfo;

public class NodeInfoSerializer {

    private NodeInfo nodeInfo;
    private String xml;

    public NodeInfoSerializer(final NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    @Override
    public String toString() {
        if (xml == null && nodeInfo != null) {
            xml = EventListUtils.getXML(nodeInfo);
            nodeInfo = null;
        }

        return xml;
    }
}
