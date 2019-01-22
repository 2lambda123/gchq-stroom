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

package stroom.cluster.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.cluster.api.ClusterNodeManager;
import stroom.cluster.api.ClusterState;
import stroom.entity.event.EntityEvent;
import stroom.entity.event.EntityEventHandler;
import stroom.entity.shared.EntityAction;
import stroom.node.NodeCache;
import stroom.node.shared.ClusterNodeInfo;
import stroom.node.shared.Node;
import stroom.task.TaskCallbackAdaptor;
import stroom.task.api.TaskManager;
import stroom.ui.config.shared.UiConfig;
import stroom.util.date.DateUtil;
import stroom.util.shared.VoidResult;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Component that remembers the node list and who is the current master node
 */
@Singleton
@EntityEventHandler(type = Node.ENTITY_TYPE, action = {EntityAction.CREATE, EntityAction.DELETE, EntityAction.UPDATE})
public class ClusterNodeManagerImpl implements ClusterNodeManager, EntityEvent.Handler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterNodeManagerImpl.class);

    private static final int ONE_SECOND = 1000;
    private static final int ONE_MINUTE = 60 * ONE_SECOND;
    private static final int TEN_MINUTES = 10 * ONE_MINUTE;
    /**
     * The amount of time in milliseconds we will delay before re-querying
     * state.
     */
    private static final int REQUERY_DELAY = 5 * ONE_SECOND;

    private final ClusterState clusterState = new ClusterState();
    private final AtomicBoolean updatingState = new AtomicBoolean();
    private final AtomicBoolean pendingUpdate = new AtomicBoolean();
    private final NodeCache nodeCache;
    private final TaskManager taskManager;
    private final UiConfig clientProperties;

    @Inject
    ClusterNodeManagerImpl(final NodeCache nodeCache,
                           final TaskManager taskManager,
                           final UiConfig clientProperties) {
        this.nodeCache = nodeCache;
        this.taskManager = taskManager;
        this.clientProperties = clientProperties;
    }

    public void init() {
        // Run initial query of cluster state.
        updateClusterStateAsync(REQUERY_DELAY, false);
    }

    /**
     * Gets the current cluster state. If the current state is null or is older
     * than the expiry time then the cluster state will be updated
     * asynchronously ready for subsequent calls. The current state is still
     * returned so could be null
     */
    @Override
    public ClusterState getClusterState() {
        // If the cluster state has never been updated then make sure we are updating it.
        if (clusterState.getUpdateTime() == 0) {
            updateClusterStateAsync(0, false);
        } else {
            // Determine if the current state should be re-evaluated as it is
            // older than 10 minutes.
            final long expiryTime = System.currentTimeMillis() - TEN_MINUTES;
            if (clusterState.getUpdateTime() < expiryTime) {
                updateClusterStateAsync(0, false);
            }
        }

        return clusterState;
    }

    private void updateClusterStateAsync(final int taskDelay, final boolean force) {
        if (force) {
            pendingUpdate.set(true);
        }

        if (updatingState.compareAndSet(false, true)) {
            doUpdate(taskDelay);
        }
    }

    private void doUpdate(final int taskDelay) {
        final UpdateClusterStateTask task = new UpdateClusterStateTask(clusterState, taskDelay, true);

        pendingUpdate.set(false);
        taskManager.execAsync(task, new TaskCallbackAdaptor<>() {
            @Override
            public void onSuccess(final VoidResult result) {
                try {
                    // Output some debug about the state we are about to set.
                    outputDebug(clusterState);

                    // Re-query cluster state if we are pending an update.
                    if (pendingUpdate.get()) {
                        // Wait a few seconds before we query again.
                        Thread.sleep(REQUERY_DELAY);
                        // Query again.
                        doUpdate(0);
                    }
                } catch (final InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);

                    // Continue to interrupt this thread.
                    Thread.currentThread().interrupt();
                } catch (final RuntimeException e) {
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    updatingState.set(false);
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                updatingState.set(false);
            }
        });
    }

    private void outputDebug(final ClusterState clusterState) {
        try {
            if (LOGGER.isDebugEnabled()) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Setting cluster state:");

                if (clusterState == null) {
                    builder.append(" NULL");

                } else {
                    final List<String> nodes = asList(clusterState.getAllNodes());
                    for (final String node : nodes) {
                        builder.append("\n\t");
                        builder.append(node);
                        if (node.equals(clusterState.getMasterNode())) {
                            builder.append("(MASTER)");
                        }
                    }
                }

                LOGGER.debug(builder.toString());
            }
        } catch (final RuntimeException e) {
            // Ignore exceptions.
        }
    }

    /**
     * Gets the current cluster state or builds a new cluster state if no
     * current state exists but without locking. This method is quick as either
     * the current state is returned or a basic state is created that does not
     * attempt to determine which nodes are active or what the current master
     * node is.
     */
    @Override
    public ClusterState getQuickClusterState() {
        // Just get the current state if we can.
        if (clusterState.getUpdateTime() == 0) {
            // Create a cluster state object but don't bother trying to contact
            // remote nodes to determine active status.
            taskManager.exec(new UpdateClusterStateTask(clusterState, 0, false));
        }
        return clusterState;
    }

    @Override
    public ClusterNodeInfo getClusterNodeInfo() {
        final ClusterState clusterState = getQuickClusterState();

        // Take a copy of our working variables;
        final List<String> allNodeList = asList(clusterState.getAllNodes());
        final List<String> activeNodeList = asList(clusterState.getEnabledActiveNodes());
        final String thisNode = nodeCache.getThisNodeName();
        final String masterNode = clusterState.getMasterNode();
        final String discoverTime = DateUtil.createNormalDateTimeString(clusterState.getUpdateTime());

        final ClusterNodeInfo clusterNodeInfo = new ClusterNodeInfo(discoverTime,
                clientProperties.getBuildDate(),
                clientProperties.getBuildVersion(),
                clientProperties.getUpDate(),
                thisNode,
                nodeCache.getClusterUrl(thisNode));

        if (allNodeList != null && activeNodeList != null && masterNode != null) {
            for (final String node : allNodeList) {
                clusterNodeInfo.addItem(node, activeNodeList.contains(node), masterNode.equals(node));
            }
        }

        return clusterNodeInfo;
    }

    @Override
    public Long ping(final String sourceNode) {
        try {
            // Update the cluster state if the node trying to ping us is not us.
            final String thisNode = nodeCache.getThisNodeName();
            if (!thisNode.equals(sourceNode)) {
                // Try and get the cluster state and make sure we know about the
                // node trying to ping us. If we can't get the cluster state or
                // the current state doesn't know the source node is active then
                // update the cluster state.
                if (clusterState.getUpdateTime() != 0) {
                    final Set<String> enabledActiveNodes = clusterState.getEnabledActiveNodes();
                    if (!enabledActiveNodes.contains(sourceNode)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                    "ping() - Just had a ping from a node that was not in our ping list.  Next cluster call we will re-discover");
                        }
                        updateClusterStateAsync(REQUERY_DELAY, true);
                    }
                } else {
                    updateClusterStateAsync(REQUERY_DELAY, true);
                }
            }
        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return System.currentTimeMillis();
    }

    /**
     * Trigger a re-discover if the node saves
     */
    @Override
    public void onChange(final EntityEvent event) {
        // Force this node to redo it's cluster state.
        updateClusterStateAsync(REQUERY_DELAY, true);
    }

    private List<String> asList(Set<String> set) {
        final List<String> list = new ArrayList<>(set);
        list.sort((o1, o2) -> {
            if (o1 != null && o2 != null) {
                return o1.compareTo(o2);
            }
            return 0;
        });
        return list;
    }
}
