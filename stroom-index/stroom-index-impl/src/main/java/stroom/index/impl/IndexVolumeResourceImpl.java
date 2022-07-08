package stroom.index.impl;

import stroom.cluster.api.EndpointUrlService;
import stroom.cluster.api.RemoteRestService;
import stroom.cluster.api.RemoteRestUtil;
import stroom.entity.shared.ExpressionCriteria;
import stroom.event.logging.rs.api.AutoLogged;
import stroom.index.shared.IndexVolume;
import stroom.index.shared.IndexVolumeResource;
import stroom.util.jersey.UriBuilderUtil;
import stroom.util.jersey.WebTargetFactory;
import stroom.util.rest.RestUtil;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.ResultPage;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@AutoLogged
class IndexVolumeResourceImpl implements IndexVolumeResource {

    private final Provider<IndexVolumeService> indexVolumeServiceProvider;
    private final Provider<EndpointUrlService> endpointUrlServiceProvider;
    private final Provider<WebTargetFactory> webTargetFactoryProvider;

    @Inject
    public IndexVolumeResourceImpl(final Provider<IndexVolumeService> indexVolumeServiceProvider,
                                   final Provider<EndpointUrlService> endpointUrlServiceProvider,
                                   final Provider<WebTargetFactory> webTargetFactoryProvider) {
        this.indexVolumeServiceProvider = indexVolumeServiceProvider;
        this.endpointUrlServiceProvider = endpointUrlServiceProvider;
        this.webTargetFactoryProvider = webTargetFactoryProvider;
    }

    @Override
    public ResultPage<IndexVolume> find(final ExpressionCriteria criteria) {
        return indexVolumeServiceProvider.get().find(criteria);
    }

    @Override
    public IndexVolume create(final IndexVolume indexVolume) {
        return indexVolumeServiceProvider.get().create(indexVolume);
    }

    @Override
    public IndexVolume fetch(final Integer id) {
        return indexVolumeServiceProvider.get().read(id);
    }

    @Override
    public IndexVolume update(final Integer id, final IndexVolume indexVolume) {
        return indexVolumeServiceProvider.get().update(indexVolume);
    }

    @Override
    public Boolean delete(final Integer id) {
        return indexVolumeServiceProvider.get().delete(id);
    }

    @Override
    public Boolean rescan(final String nodeName) {
        RestUtil.requireNonNull(nodeName, "nodeName not supplied");

        // If this is the node that was contacted then just resolve it locally
        final EndpointUrlService endpointUrlService = endpointUrlServiceProvider.get();
        if (endpointUrlService.shouldExecuteLocally(nodeName)) {
            indexVolumeServiceProvider.get().rescan();
        } else {
            final String url = endpointUrlService.getRemoteEndpointUrl(nodeName)
                    + ResourcePaths.buildAuthenticatedApiPath(
                    IndexVolumeResource.BASE_PATH,
                    IndexVolumeResource.RESCAN_SUB_PATH);
            try {
                // A different node to make a rest call to the required node
                WebTarget webTarget = webTargetFactoryProvider.get().create(url);
                webTarget = UriBuilderUtil.addParam(webTarget, "nodeName", nodeName);
                final Response response = webTarget
                        .request(MediaType.APPLICATION_JSON)
                        .get();
                if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    throw new NotFoundException(response);
                } else if (response.getStatus() != Status.OK.getStatusCode()) {
                    throw new WebApplicationException(response);
                }

                return response.readEntity(Boolean.class);
            } catch (final Throwable e) {
                throw RemoteRestUtil.handleExceptionsOnNodeCall(nodeName, url, e);
            }
        }

        return true;
    }
}
