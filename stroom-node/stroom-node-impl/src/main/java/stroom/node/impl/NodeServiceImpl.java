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

package stroom.node.impl;


import stroom.entity.NamedEntityServiceImpl;
import stroom.entity.StroomEntityManager;
import stroom.entity.shared.BaseResultList;
import stroom.node.api.NodeService;
import stroom.node.shared.FindNodeCriteria;
import stroom.node.shared.Node;
import stroom.node.shared.Rack;
import stroom.security.Security;
import stroom.security.shared.PermissionNames;
import stroom.ui.config.shared.UiConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * <p>
 * JPA implementation of a node manager.
 * </p>
 */
@Singleton
public class NodeServiceImpl extends NamedEntityServiceImpl<Node, FindNodeCriteria> implements NodeService {
    private final Security security;
    private final NodeServiceTransactionHelper nodeServiceUtil;

    @Inject
    NodeServiceImpl(final StroomEntityManager entityManager,
                    final Security security,
                    final UiConfig uiConfig,
                    final NodeServiceTransactionHelper nodeServiceUtil) {
        super(entityManager, security, uiConfig);
        this.security = security;
        this.nodeServiceUtil = nodeServiceUtil;
    }

    Rack getRack(final String name) {
        return nodeServiceUtil.getRack(name);
    }

    @Override
    public BaseResultList<Node> find(final FindNodeCriteria criteria) {
        return security.insecureResult(() -> super.find(criteria));
    }

    @Override
    public Class<Node> getEntityClass() {
        return Node.class;
    }

    @Override
    public FindNodeCriteria createCriteria() {
        return new FindNodeCriteria();
    }

    @Override
    protected String permission() {
        return PermissionNames.MANAGE_NODES_PERMISSION;
    }

    @Override
    public String getClusterUrl(final String nodeName) {
        final Node node = nodeServiceUtil.getNode(nodeName);
        if (node != null) {
            return node.getClusterURL();
        }
        return null;
    }

    @Override
    public boolean isEnabled(final String nodeName) {
        final Node node = nodeServiceUtil.getNode(nodeName);
        if (node != null) {
            return node.isEnabled();
        }
        return false;
    }

    @Override
    public int getPriority(final String nodeName) {
        final Node node = nodeServiceUtil.getNode(nodeName);
        if (node != null) {
            return node.getPriority();
        }
        return -1;
    }

    @Override
    public Node getNode(final String nodeName) {
        return nodeServiceUtil.getNode(nodeName);
    }
}
