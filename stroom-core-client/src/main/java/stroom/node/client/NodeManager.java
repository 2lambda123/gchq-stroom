package stroom.node.client;

import stroom.dispatch.client.RestFactory;
import stroom.node.shared.ClusterNodeInfo;
import stroom.node.shared.FetchNodeStatusResponse;
import stroom.node.shared.FindNodeStatusCriteria;
import stroom.node.shared.NodeResource;

import com.google.gwt.core.client.GWT;

import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NodeManager {

    private static final NodeResource NODE_RESOURCE = GWT.create(NodeResource.class);

    private final RestFactory restFactory;

    @Inject
    NodeManager(final RestFactory restFactory) {
        this.restFactory = restFactory;
    }

    public void fetchNodeStatus(final Consumer<FetchNodeStatusResponse> dataConsumer,
                                final Consumer<Throwable> throwableConsumer,
                                final FindNodeStatusCriteria findNodeStatusCriteria) {
        restFactory
                .forType(FetchNodeStatusResponse.class)
                .onSuccess(dataConsumer)
                .onFailure(throwableConsumer)
                .call(NODE_RESOURCE)
                .find(findNodeStatusCriteria);
    }

    public void ping(final String nodeName,
                     final Consumer<Long> pingConsumer,
                     final Consumer<Throwable> throwableConsumer) {
        restFactory
                .forType(Long.class)
                .onSuccess(pingConsumer)
                .onFailure(throwableConsumer)
                .call(NODE_RESOURCE)
                .ping(nodeName);
    }

    public void info(final String nodeName,
                     final Consumer<ClusterNodeInfo> infoConsumer,
                     final Consumer<Throwable> throwableConsumer) {
        restFactory
                .forType(ClusterNodeInfo.class)
                .onSuccess(infoConsumer)
                .onFailure(throwableConsumer)
                .call(NODE_RESOURCE)
                .info(nodeName);
    }

    public void setPriority(final String nodeName,
                            final int priority,
                            final Consumer<Boolean> resultConsumer) {
        restFactory
                .forBoolean()
                .onSuccess(resultConsumer)
                .call(NODE_RESOURCE)
                .setPriority(nodeName, priority);
    }

    public void setEnabled(final String nodeName,
                           final boolean enabled,
                           final Consumer<Boolean> resultConsumer) {
        restFactory
                .forBoolean()
                .onSuccess(resultConsumer)
                .call(NODE_RESOURCE)
                .setEnabled(nodeName, enabled);
    }

    public void listAllNodes(final Consumer<List<String>> nodeListConsumer,
                             final Consumer<Throwable> throwableConsumer) {
        restFactory
                .forStringList()
                .onSuccess(nodeListConsumer)
                .onFailure(throwableConsumer)
                .call(NODE_RESOURCE)
                .listAllNodes();
    }

    public void listEnabledNodes(final Consumer<List<String>> nodeListConsumer,
                                 final Consumer<Throwable> throwableConsumer) {
        restFactory
                .forStringList()
                .onSuccess(nodeListConsumer)
                .onFailure(throwableConsumer)
                .call(NODE_RESOURCE)
                .listEnabledNodes();
    }
}
