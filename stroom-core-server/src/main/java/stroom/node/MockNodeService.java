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

package stroom.node;

import stroom.entity.MockNamedEntityService;
import stroom.entity.shared.BaseResultList;
import stroom.node.shared.FindNodeCriteria;
import stroom.node.shared.Node;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock class that manages one node.
 */
@Singleton
public class MockNodeService extends MockNamedEntityService<Node, FindNodeCriteria>
        implements NodeService, LocalNodeProvider {
    private Node node = null;

    @Override
    public BaseResultList<Node> find(final FindNodeCriteria criteria) {
        final List<Node> nodeList = new ArrayList<>();
        nodeList.add(get());
        return new BaseResultList<>(nodeList, 0L, (long) nodeList.size(), true);
    }

    @Override
    public Node get() {
        if (node == null) {
            node = new Node();
            node.setName("MockNode");
        }
        return node;
    }

    @Override
    public String getClusterUrl(final String nodeName) {
        final Node node = get();
        if (node != null) {
            return node.getClusterURL();
        }
        return null;
    }

    @Override
    public boolean isEnabled(final String nodeName) {
        final Node node = get();
        if (node != null) {
            return node.isEnabled();
        }
        return false;
    }

    @Override
    public int getPriority(final String nodeName) {
        final Node node = get();
        if (node != null) {
            return node.getPriority();
        }
        return 0;
    }

    @Override
    public Node getNode(final String nodeName) {
        return get();
    }

    @Override
    public Class<Node> getEntityClass() {
        return Node.class;
    }
}
