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

package stroom.node;

import stroom.node.api.NodeInfo;
import stroom.node.shared.NodeInfoResult;
import stroom.security.Security;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;


class NodeInfoClusterHandler extends AbstractTaskHandler<NodeInfoClusterTask, NodeInfoResult> {
    private final NodeInfo nodeInfo;
    private final Security security;

    @Inject
    NodeInfoClusterHandler(final NodeInfo nodeInfo,
                           final Security security) {
        this.nodeInfo = nodeInfo;
        this.security = security;
    }

    @Override
    public NodeInfoResult exec(final NodeInfoClusterTask action) {
        return security.secureResult(() -> {
            final NodeInfoResult nodeInfoResult = new NodeInfoResult();
            nodeInfoResult.setEntity(nodeInfo.getThisNode());
            nodeInfoResult.setPing(System.currentTimeMillis());
            return nodeInfoResult;
        });
    }
}
