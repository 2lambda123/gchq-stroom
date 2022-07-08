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

package stroom.cluster.task.impl;

import stroom.cluster.api.ClusterService;
import stroom.cluster.task.api.NodeNotFoundException;
import stroom.cluster.task.api.TargetNodeSetFactory;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TargetNodeSetFactoryImpl implements TargetNodeSetFactory {

    private final ClusterService clusterService;

    @Inject
    public TargetNodeSetFactoryImpl(final ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @Override
    public String getSourceNode() {
        return clusterService.getLocalNodeName().orElseThrow(() ->
                new RuntimeException("Local node cannot be found"));
    }

    @Override
    public String getLeaderNode() throws NodeNotFoundException {
        return clusterService.getLeaderNodeName().orElseThrow(() ->
                new NodeNotFoundException("Leader node cannot be found"));
    }

    @Override
    public Set<String> getEnabledActiveTargetNodeSet() {
        return clusterService.getNodeNames();
    }

    @Override
    public boolean isClusterStateInitialised() {
        return true;
    }
}
