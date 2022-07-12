package stroom.test.common.util.test;

import stroom.cluster.api.ClusterMember;
import stroom.util.jersey.WebTargetFactory;
import stroom.util.logging.LogUtil;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.dropwizard.jersey.errors.ErrorMessage;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An abstract test class for use when testing a resource that is running on multiple nodes.
 * Uses Grizzly http server to serve the resources. It is therefore not possible to debug the
 * server side code.
 */
@ExtendWith(MockitoExtension.class)
public abstract class AbstractMultiNodeResourceTest<R extends RestResource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMultiNodeResourceTest.class);

    private static final WebTargetFactory WEB_TARGET_FACTORY = url -> ClientBuilder.newClient(
                    new ClientConfig().register(LoggingFeature.class))
            .target(url);

    private static final String CONTAINER_FACTORY = "org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory";

    private final List<TestMember> members;
    private final Map<String, JerseyTest> nodeToJerseyTestMap = new HashMap<>();
    private final Map<String, RequestListener> nodeToListenerMap = new HashMap<>();

    public static List<TestMember> createNodeList(final int base) {
        return List.of(
                new TestMember(new ClusterMember("node1"), base, true),
                new TestMember(new ClusterMember("node2"), base + 1, true),
                new TestMember(new ClusterMember("node3"), base + 2, false));
    }

    /**
     * Uses the supplied nodes for testing.
     */
    @SuppressWarnings("unused")
    protected AbstractMultiNodeResourceTest(final List<TestMember> members) {

        // Force the container factory to ensure the jersey-test-framework-provider-grizzly2
        // dependency is in place. Without forcing it, it will just try to use whatever is there
        // which may be the in memory one which won't work for multi node.
        // Tried using the jetty container factory but there was as dependency version mismatch
        // that causes a method not found error.
        System.setProperty(TestProperties.CONTAINER_FACTORY, CONTAINER_FACTORY);

        try {
            Class.forName(CONTAINER_FACTORY);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("You are missing a test runtime dependency for " +
                    "jersey-test-framework-provider-grizzly2");
        }

        this.members = members;
    }

    /**
     * @return The base path of the resource, e.g. /node/v1
     */
    public abstract String getResourceBasePath();

    /**
     * This will be called during initNodes() and provides a means for the sub-class
     * to provide a fully mocked out implementation of the rest resource. The arguments
     * are provided so you can have mocks tailored to the node.
     */
    public abstract R getRestResource(final TestMember local,
                                      final List<TestMember> members);

    private String getFullResourcePath() {
        return ResourcePaths.buildAuthenticatedApiPath(getResourceBasePath());
    }

//    public String getBaseEndPointUrl(final ClusterMember member) {
//        return "http://localhost:" + node.getPort();
//    }
//
//    private Map<ClusterMember, TestNode> getBaseEndPointUrls() {
//        return testNodes.stream()
//                .collect(Collectors.toMap(
//                        TestNode::getNodeName,
//                        this::getBaseEndPointUrl));
//    }

    public void stopNodes() {
        nodeToJerseyTestMap.values().forEach(jerseyTest -> {
            try {
                jerseyTest.tearDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @AfterEach
    void afterEach() {
        stopNodes();
        nodeToJerseyTestMap.clear();
        nodeToListenerMap.clear();
    }

    /**
     * Initialises all nodes the getTestNodes()
     * Calls getRestResource() for each node initialised.
     */
    protected void initNodes() {
        initNodes(Integer.MAX_VALUE);
    }

    /**
     * Initialises the fist node in getTestNodes()
     * For use when your test does not require the calling out to other nodes.
     * Calls getRestResource() for the node being initialised.
     */
    protected void initSingleNode() {
        initNodes(1);
    }

    private void initNodes(final int maxNodeCount) {

        members
                .stream()
                .limit(maxNodeCount)
                .forEach(node -> {

                    final String baseEndPointUrl = node.getEndpointUrl();

                    RequestListener requestListener = new RequestListener(node);
                    nodeToListenerMap.put(node.getUuid(), requestListener);

                    final JerseyTest jerseyTest = new JerseyTestBuilder<>(
                            () -> getRestResource(node, members),
                            node.getPort(),
                            requestListener)
                            .build();

                    nodeToJerseyTestMap.put(node.getUuid(), jerseyTest);

                    try {
                        if (node.isEnabled) {
                            LOGGER.info("Starting node [{}] (enabled: {}) at {}",
                                    node.getUuid(), node.isEnabled, baseEndPointUrl);
                            jerseyTest.setUp();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Error starting jersey test on " + baseEndPointUrl, e);
                    }
                });
    }

    /**
     * Override if you want to use more nodes or different ports
     */
    public List<TestMember> getTestNodes() {
        return members;
    }

    public List<RequestEvent> getRequestEvents(final String nodeName) {
        return nodeToListenerMap.get(nodeName).getRequestLog();
    }

    /**
     * @return The JerseyTest instance for the first node
     */
    public JerseyTest getJerseyTest() {
        return nodeToJerseyTestMap.get(members.get(0).getUuid());
    }

    /**
     * @return The JerseyTest instance for the first node
     */
    public JerseyTest getJerseyTest(final String nodeName) {
        return nodeToJerseyTestMap.get(nodeName);
    }

    public static WebTargetFactory webTargetFactory() {
        return WEB_TARGET_FACTORY;
    }

    public <T_RESP> T_RESP doGetTest(final String subPath,
                                     final Class<T_RESP> responseType,
                                     final T_RESP expectedResponse,
                                     final Function<WebTarget, WebTarget>... builderMethods) {
        LOGGER.info("Calling GET on {}{}, expecting {}",
                getResourceBasePath(), subPath, expectedResponse);

        return doTest(Invocation.Builder::get,
                subPath,
                responseType,
                expectedResponse,
                builderMethods);
    }

    public <T_REQ, T_RESP> T_RESP doPostTest(final String subPath,
                                             final T_REQ requestEntity,
                                             final Class<T_RESP> responseType,
                                             final T_RESP expectedResponse,
                                             final Function<WebTarget, WebTarget>... builderMethods) {
        LOGGER.info("Calling POST on {}{}, expecting {}",
                getResourceBasePath(), subPath, expectedResponse);

        return doTest(builder -> builder.post(Entity.json(requestEntity)),
                subPath,
                responseType,
                expectedResponse,
                builderMethods);
    }

    public <T_REQ> void doPostTest(final String subPath,
                                   final T_REQ requestEntity,
                                   final Function<WebTarget, WebTarget>... builderMethods) {

        LOGGER.info("Calling POST on {}{}, passing {}",
                getResourceBasePath(), subPath, requestEntity);

        WebTarget webTarget = getJerseyTest()
                .target(getResourceBasePath())
                .path(subPath);

        for (Function<WebTarget, WebTarget> method : builderMethods) {
            webTarget = method.apply(webTarget);
        }

        Invocation.Builder builder = webTarget
                .request();

        Response response = builder.post(Entity.json(requestEntity));

        if (!isSuccessful(response.getStatus())) {
            throw new RuntimeException(LogUtil.message("Error: {} {}", response.getStatus(), response));
        }
    }

    public <T_REQ, T_RESP> T_RESP doPutTest(final String subPath,
                                            final T_REQ requestEntity,
                                            final Class<T_RESP> responseType,
                                            final T_RESP expectedResponse,
                                            final Function<WebTarget, WebTarget>... builderMethods) {
        LOGGER.info("Calling PUT on {}{}, expecting {}",
                getResourceBasePath(), subPath, expectedResponse);

        return doTest(builder -> builder.put(Entity.json(requestEntity)),
                subPath,
                responseType,
                expectedResponse,
                builderMethods);
    }

    public <T_REQ> void doPutTest(final String subPath,
                                  final T_REQ requestEntity,
                                  final Function<WebTarget, WebTarget>... builderMethods) {

        LOGGER.info("Calling PUT on {}{}, passing {}",
                getResourceBasePath(), subPath, requestEntity);

        WebTarget webTarget = getJerseyTest()
                .target(getResourceBasePath())
                .path(subPath);

        for (Function<WebTarget, WebTarget> method : builderMethods) {
            webTarget = method.apply(webTarget);
        }

        Invocation.Builder builder = webTarget
                .request();

        Response response = builder.put(Entity.json(requestEntity));

        if (!isSuccessful(response.getStatus())) {
            throw new RuntimeException(LogUtil.message("Error: {} {}", response.getStatus(), response));
        }
    }

    public <T_RESP> T_RESP doDeleteTest(final String subPath,
                                        final Class<T_RESP> responseType,
                                        final T_RESP expectedResponse,
                                        final Function<WebTarget, WebTarget>... builderMethods) {
        LOGGER.info("Calling DELETE on {}{}, expecting {}",
                getResourceBasePath(), subPath, expectedResponse);

        return doTest(Invocation.Builder::delete,
                subPath,
                responseType,
                expectedResponse,
                builderMethods);
    }

    private <T_RESP> T_RESP doTest(final Function<Invocation.Builder, Response> operation,
                                   final String subPath,
                                   final Class<T_RESP> responseType,
                                   final T_RESP expectedResponse,
                                   final Function<WebTarget, WebTarget>... builderMethods) {
        WebTarget webTarget = getJerseyTest()
                .target(getResourceBasePath())
                .path(subPath);

        for (Function<WebTarget, WebTarget> method : builderMethods) {
            webTarget = method.apply(webTarget);
        }

        final Invocation.Builder builder = webTarget
                .request();

        final Response response = operation.apply(builder);

        if (!isSuccessful(response.getStatus())) {
            throw new RuntimeException(LogUtil.message("Error: {} {}",
                    response.getStatus(), response));
        }

        final T_RESP entity = response.readEntity(responseType);

        if (expectedResponse != null) {
            Assertions.assertThat(entity)
                    .isEqualTo(expectedResponse);
        }

        return entity;
    }

    private <T_REQ, T_RESP> T_RESP doTest(final Function<Invocation.Builder, Response> operation,
                                          final String subPath,
                                          final T_REQ requestEntity,
                                          final Class<T_RESP> responseType,
                                          final T_RESP expectedResponse,
                                          final Function<WebTarget, WebTarget>... builderMethods) {
        LOGGER.info("Calling GET on {}{}, expecting {}",
                getResourceBasePath(), subPath, expectedResponse);

        WebTarget webTarget = getJerseyTest()
                .target(getResourceBasePath())
                .path(subPath);

        for (Function<WebTarget, WebTarget> method : builderMethods) {
            webTarget = method.apply(webTarget);
        }

        Invocation.Builder builder = webTarget
                .request();

        final Response response = operation.apply(builder);

        final T_RESP entity = response.readEntity(responseType);

        if (expectedResponse != null) {
            Assertions.assertThat(response)
                    .isEqualTo(expectedResponse);
        }

        return entity;
    }


    public WebTarget getWebTarget(final String subPath) {

        return getJerseyTest()
                .target(getFullResourcePath())
                .path(subPath);
    }

    public static <T> T createNamedMock(final Class<T> clazz, final TestMember node) {
        return Mockito.mock(clazz, clazz.getName() + "_" + node.getUuid());
    }

    private boolean isSuccessful(final int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private static class JerseyTestBuilder<R extends RestResource> {

        private final Supplier<R> resourceSupplier;
        private final int port;
        private final ApplicationEventListener listener;

        public JerseyTestBuilder(final Supplier<R> resourceSupplier,
                                 final int port,
                                 final ApplicationEventListener listener) {
            this.resourceSupplier = resourceSupplier;
            this.port = port;
            this.listener = listener;
        }

        public JerseyTest build() {
            return new JerseyTest() {

                @Override
                protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
                    return super.getTestContainerFactory();

                }

                @Override
                protected Application configure() {
                    final LoggingFeature loggingFeature = new LoggingFeature(
                            java.util.logging.Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME),
                            Level.INFO,
                            LoggingFeature.Verbosity.PAYLOAD_ANY,
                            LoggingFeature.DEFAULT_MAX_ENTITY_SIZE);

                    return new ResourceConfig()
                            .register(resourceSupplier.get())
                            .register(listener)
                            .register(new MyExceptionMapper()) // So we can get details of server side exceptions
                            .register(loggingFeature);
                }

                @Override
                protected URI getBaseUri() {
                    return UriBuilder
                            .fromUri("http://localhost")
                            .port(port)
                            .path(ResourcePaths.API_ROOT_PATH)
                            .build();
                }
            };
        }
    }

    public static class TestMember {

        private final ClusterMember member;
        private final int port;
        private final boolean isEnabled;

        public TestMember(final ClusterMember member,
                          final int port,
                          final boolean isEnabled) {
            this.member = member;
            this.port = port;
            this.isEnabled = isEnabled;
        }

        public ClusterMember getMember() {
            return member;
        }

        public String getUuid() {
            return member.getUuid();
        }

        public int getPort() {
            return port;
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        public String getEndpointUrl() {
            return "http://localhost:" + port;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TestMember testMember = (TestMember) o;
            return port == testMember.port &&
                    isEnabled == testMember.isEnabled &&
                    Objects.equals(member, testMember.member);
        }

        @Override
        public int hashCode() {
            return Objects.hash(member, port, isEnabled);
        }

        @Override
        public String toString() {
            return "TestNode{" +
                    "member='" + member + '\'' +
                    ", port=" + port +
                    ", isEnabled=" + isEnabled +
                    '}';
        }
    }

    public static class RequestListener implements ApplicationEventListener {

        private final List<RequestEvent> requestLog = new ArrayList<>();
        private final TestMember node;

        RequestListener(final TestMember node) {
            this.node = node;
        }

        @Override
        public void onEvent(final ApplicationEvent event) {
            LOGGER.debug("ApplicationEvent on member {}", node.getUuid());
        }

        @Override
        public RequestEventListener onRequest(final RequestEvent requestEvent) {
            LOGGER.debug("{} to {} request received on member {} ",
                    requestEvent.getType(), requestEvent.getUriInfo().getPath(), node.getUuid());

            requestLog.add(requestEvent);
            return null;
        }

        public List<RequestEvent> getRequestLog() {
            return requestLog;
        }
    }

    private static class MyExceptionMapper implements ExceptionMapper<Throwable> {

        private static final Logger LOGGER = LoggerFactory.getLogger(MyExceptionMapper.class);

        @Override
        public Response toResponse(final Throwable exception) {
            if (exception instanceof WebApplicationException) {
                WebApplicationException wae = (WebApplicationException) exception;
                return wae.getResponse();
            } else {
                return createExceptionResponse(Status.INTERNAL_SERVER_ERROR, exception);
            }
        }

        private Response createExceptionResponse(final Response.Status status,
                                                 final Throwable throwable) {
            LOGGER.debug(throwable.getMessage(), throwable);
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            throwable.getMessage(),
                            throwable.toString()))
                    .build();
        }
    }

}
