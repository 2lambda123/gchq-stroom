package stroom.index.impl;

import stroom.docref.DocRef;
import stroom.event.logging.rs.api.AutoLogged;
import stroom.index.impl.IndexShardManager.IndexShardAction;
import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.IndexDoc;
import stroom.index.shared.IndexResource;
import stroom.index.shared.IndexShard;
import stroom.node.api.NodeCallUtil;
import stroom.node.api.NodeInfo;
import stroom.node.api.NodeService;
import stroom.util.jersey.WebTargetFactory;
import stroom.util.rest.RestUtil;
import stroom.util.shared.EntityServiceException;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.ResultPage;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@AutoLogged
class IndexResourceImpl implements IndexResource {

    private final IndexStore indexStore;
    private final IndexShardService indexShardService;
    private final IndexShardManager indexShardManager;
    private final NodeService nodeService;
    private final NodeInfo nodeInfo;
    private final WebTargetFactory webTargetFactory;

    @Inject
    IndexResourceImpl(final IndexStore indexStore,
                      final IndexShardService indexShardService,
                      final IndexShardManager indexShardManager,
                      final NodeService nodeService,
                      final NodeInfo nodeInfo,
                      final WebTargetFactory webTargetFactory) {
        this.indexStore = indexStore;
        this.indexShardService = indexShardService;
        this.indexShardManager = indexShardManager;
        this.nodeService = nodeService;
        this.nodeInfo = nodeInfo;
        this.webTargetFactory = webTargetFactory;
    }

    @Override
    public IndexDoc fetch(final String uuid) {
        return indexStore.readDocument(getDocRef(uuid));
    }

    @Override
    public IndexDoc update(final String uuid, final IndexDoc doc) {
        if (doc.getUuid() == null || !doc.getUuid().equals(uuid)) {
            throw new EntityServiceException("The document UUID must match the update UUID");
        }
        return indexStore.writeDocument(doc);
    }

    private DocRef getDocRef(final String uuid) {
        return DocRef.builder()
                .uuid(uuid)
                .type(IndexDoc.DOCUMENT_TYPE)
                .build();
    }

    @Override
    public ResultPage<IndexShard> findIndexShards(final FindIndexShardCriteria criteria) {
        return indexShardService.find(criteria);
    }

    @Override
    public Long deleteIndexShards(final String nodeName, final FindIndexShardCriteria criteria) {
        return performShardAction(nodeName, criteria, IndexResource.SHARD_DELETE_SUB_PATH, IndexShardAction.DELETE);
    }

    @Override
    public Long flushIndexShards(final String nodeName, final FindIndexShardCriteria criteria) {
        return performShardAction(nodeName, criteria, IndexResource.SHARD_FLUSH_SUB_PATH, IndexShardAction.FLUSH);
    }

    private Long performShardAction(final String nodeName,
                                    final FindIndexShardCriteria criteria,
                                    final String subPath,
                                    final IndexShardAction action) {
        RestUtil.requireNonNull(nodeName, "nodeName not supplied");

        // If this is the node that was contacted then just resolve it locally
        if (NodeCallUtil.shouldExecuteLocally(nodeInfo, nodeName)) {
            return indexShardManager.performAction(criteria, action);
        } else {
            final String url = NodeCallUtil.getBaseEndpointUrl(nodeInfo, nodeService, nodeName)
                    + ResourcePaths.buildAuthenticatedApiPath(
                    IndexResource.BASE_PATH,
                    subPath);
            try {
                // A different node to make a rest call to the required node
                final Response response = webTargetFactory
                        .create(url)
                        .queryParam("nodeName", nodeName)
                        .request(MediaType.APPLICATION_JSON)
                        .post(Entity.json(criteria));
                if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    throw new NotFoundException(response);
                } else if (response.getStatus() != Status.OK.getStatusCode()) {
                    throw new WebApplicationException(response);
                }

                return response.readEntity(Long.class);
            } catch (final Throwable e) {
                throw NodeCallUtil.handleExceptionsOnNodeCall(nodeName, url, e);
            }
        }
    }
}
