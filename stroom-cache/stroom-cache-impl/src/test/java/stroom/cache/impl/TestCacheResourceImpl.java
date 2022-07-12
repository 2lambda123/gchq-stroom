package stroom.cache.impl;


import stroom.cache.shared.CacheInfo;
import stroom.cache.shared.CacheInfoResponse;
import stroom.cache.shared.CacheNamesResponse;
import stroom.cache.shared.CacheResource;
import stroom.cluster.api.ClusterService;
import stroom.cluster.api.EndpointUrlService;
import stroom.task.api.SimpleTaskContextFactory;
import stroom.test.common.util.test.AbstractMultiNodeResourceTest;
import stroom.test.common.util.test.MockClusterService;
import stroom.test.common.util.test.MockEndpointUrlService;
import stroom.util.jersey.UriBuilderUtil;
import stroom.util.shared.ResourcePaths;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
class TestCacheResourceImpl extends AbstractMultiNodeResourceTest<CacheResource> {

    @Mock
    private CacheManagerService cacheManagerService;

    private final Map<String, CacheManagerService> cacheManagerServiceMocks = new HashMap<>();

    private static final int BASE_PORT = 7010;

    public TestCacheResourceImpl() {
        super(createNodeList(BASE_PORT));
    }

    @Override
    public String getResourceBasePath() {
        return CacheResource.BASE_PATH;
    }

    @Override
    public CacheResource getRestResource(final TestMember local,
                                         final List<TestMember> members) {

        // Set up the NodeService mock
        final EndpointUrlService endpointUrlService = new MockEndpointUrlService(local, members);

        // Set up the CacheManagerService mock

        final CacheManagerService cacheManagerService = Mockito.mock(CacheManagerService.class,
                CacheManagerService.class.getName() + "_" + local.getUuid());
        final ClusterService clusterService = new MockClusterService(local, members);

        cacheManagerServiceMocks.put(local.getUuid(), cacheManagerService);

        when(cacheManagerService.getCacheNames())
                .thenReturn(List.of("cache1", "cache2"));

        when(cacheManagerService.find(Mockito.any(FindCacheInfoCriteria.class)))
                .thenAnswer(invocation -> {
                    FindCacheInfoCriteria criteria = (invocation.getArgument(0));
                    if (criteria.getName().isConstrained()) {
                        return List.of(new CacheInfo(criteria.getName().getString(),
                                Collections.emptyMap(),
                                local.getUuid()));
                    } else {
                        return List.of(
                                new CacheInfo("cache1", Collections.emptyMap(), local.getUuid()),
                                new CacheInfo("cache2", Collections.emptyMap(), local.getUuid()));
                    }
                });

        // Now create the service

        return new CacheResourceImpl(
                () -> endpointUrlService,
                AbstractMultiNodeResourceTest::webTargetFactory,
                () -> cacheManagerService,
                SimpleTaskContextFactory::new,
                () -> clusterService);
    }

    @Test
    void list_sameNode() {
        final String subPath = ResourcePaths.buildPath(CacheResource.LIST);

        final List<String> caches = List.of("cache1", "cache2");
        final CacheNamesResponse expectedResponse = new CacheNamesResponse(caches);

        initNodes();

        when(cacheManagerServiceMocks.get("node1").getCacheNames())
                .thenReturn(caches);

        doGetTest(
                subPath,
                CacheNamesResponse.class,
                expectedResponse,
                webTarget -> UriBuilderUtil.addParam(webTarget, "memberUuid", "node1"));
    }

    @Test
    void list_otherNode() {
        final String subPath = ResourcePaths.buildPath(CacheResource.LIST);

        final List<String> caches = List.of("cache1", "cache2");
        final CacheNamesResponse expectedResponse = new CacheNamesResponse(caches);

        initNodes();

        when(cacheManagerServiceMocks.get("node2").getCacheNames())
                .thenReturn(caches);

        doGetTest(
                subPath,
                CacheNamesResponse.class,
                expectedResponse,
                webTarget -> UriBuilderUtil.addParam(webTarget, "memberUuid", "node2"));
    }

    @Test
    void info_sameNode() {
        final String subPath = ResourcePaths.buildPath(CacheResource.INFO);

        List<CacheInfo> cacheInfoList = List.of(
                new CacheInfo("cache1", Collections.emptyMap(), "node1"));

        final CacheInfoResponse expectedResponse = new CacheInfoResponse(cacheInfoList);

        initNodes();

        when(cacheManagerService.find(Mockito.any()))
                .thenReturn(cacheInfoList);

        doGetTest(
                subPath,
                CacheInfoResponse.class,
                expectedResponse,
                webTarget -> UriBuilderUtil.addParam(webTarget, "cacheName", "cache1"),
                webTarget -> UriBuilderUtil.addParam(webTarget, "memberUuid", "node1"));
    }

    @Test
    void info_otherNode() {
        final String subPath = ResourcePaths.buildPath(CacheResource.INFO);

        List<CacheInfo> cacheInfoList = List.of(
                new CacheInfo("cache1", Collections.emptyMap(), "node2"));

        final CacheInfoResponse expectedResponse = new CacheInfoResponse(cacheInfoList);

        initNodes();

        when(cacheManagerService.find(Mockito.any()))
                .thenReturn(cacheInfoList);

        doGetTest(
                subPath,
                CacheInfoResponse.class,
                expectedResponse,
                webTarget -> UriBuilderUtil.addParam(webTarget, "cacheName", "cache1"),
                webTarget -> UriBuilderUtil.addParam(webTarget, "memberUuid", "node2"));
    }

    @Test
    void clear_sameNode() {
        final String subPath = "";

        final Long expectedResponse = 1L;

        initNodes();

        when(cacheManagerServiceMocks.get("node1").clear(Mockito.any(FindCacheInfoCriteria.class)))
                .thenReturn(1L);

        doDeleteTest(
                subPath,
                Long.class,
                expectedResponse,
                webTarget -> UriBuilderUtil.addParam(webTarget, "cacheName", "cache1"),
                webTarget -> UriBuilderUtil.addParam(webTarget, "memberUuid", "node1"));

        verify(cacheManagerServiceMocks.get("node1"))
                .clear(Mockito.any());
        verify(cacheManagerServiceMocks.get("node2"), Mockito.never())
                .clear(Mockito.any());
        verify(cacheManagerServiceMocks.get("node3"), Mockito.never())
                .clear(Mockito.any());
    }

    @Test
    void clear_otherNode() {
        final String subPath = "";

        final Long expectedResponse = 1L;

        initNodes();

        when(cacheManagerServiceMocks.get("node2").clear(Mockito.any(FindCacheInfoCriteria.class)))
                .thenReturn(1L);

        doDeleteTest(
                subPath,
                Long.class,
                expectedResponse,
                webTarget -> UriBuilderUtil.addParam(webTarget, "cacheName", "cache1"),
                webTarget -> UriBuilderUtil.addParam(webTarget, "memberUuid", "node2"));

        verify(cacheManagerServiceMocks.get("node1"), Mockito.never())
                .clear(Mockito.any());
        verify(cacheManagerServiceMocks.get("node2"))
                .clear(Mockito.any());
        verify(cacheManagerServiceMocks.get("node3"), Mockito.never())
                .clear(Mockito.any());
    }

}
