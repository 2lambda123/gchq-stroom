package stroom.proxy.app;

import stroom.util.logging.LogUtil;
import stroom.util.shared.ResourcePaths;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.google.inject.Injector;
import io.dropwizard.server.DefaultServerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;


public class AbstractEndToEndTest extends AbstractApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEndToEndTest.class);

    final MockHttpDestination mockHttpDestination = new MockHttpDestination();

    // Use RegisterExtension instead of @WireMockTest so we can set up the req listener
    @SuppressWarnings("unused")
    @RegisterExtension
    public final WireMockExtension wireMockExtension = mockHttpDestination.createExtension();

    @BeforeEach
    void setup(final WireMockRuntimeInfo wmRuntimeInfo) {
        LOGGER.info("WireMock running on: {}", wmRuntimeInfo.getHttpBaseUrl());
        mockHttpDestination.clear();

        final App app = getDropwizard().getApplication();
        final Injector injector = app.getInjector();
        injector.injectMembers(this);
    }

    public PostDataHelper createPostDataHelper() {
        final String url = buildProxyAppPath(ResourcePaths.buildUnauthenticatedServletPath("datafeed"));
        return new PostDataHelper(getClient(), url);
    }

    void waitForHealthyProxyApp(final Duration timeout) {
        final Instant startTime = Instant.now();
        final String healthCheckUrl = buildProxyAdminPath("/healthcheck");

        boolean didTimeout = true;
        Response response = null;

        LOGGER.info("Waiting for proxy to start using " + healthCheckUrl);
        while (startTime.plus(timeout).isAfter(Instant.now())) {
            try {
                response = getClient().target(healthCheckUrl)
                        .request()
                        .get();
                if (Family.SUCCESSFUL.equals(response.getStatusInfo().toEnum().getFamily())) {
                    didTimeout = false;
                    LOGGER.info("Proxy is ready and healthy");
                    break;
                } else {
                    throw new RuntimeException(LogUtil.message("Proxy is unhealthy, got {} code",
                            response.getStatus()));
                }
            } catch (Exception e) {
                // Expected, so sleep and go round again
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while sleeping");
            }
        }
        if (didTimeout) {
            // Get the health check content so we can see what is wrong. Likely a feed status check issue
            final Map<String, Object> map = response.readEntity(new GenericType<Map<String, Object>>() {
            });
            throw new RuntimeException(LogUtil.message(
                    "Timed out waiting for proxy to start. Last response: {}", map));
        }
    }

    String getProxyBaseAppUrl() {
        final String appPath = ((DefaultServerFactory) getConfig().getServerFactory()).getApplicationContextPath();
        return "http://localhost:" + getDropwizard().getLocalPort() + appPath;
    }

    String getProxyBaseAdminUrl() {
        final String adminPath = ((DefaultServerFactory) getConfig().getServerFactory()).getAdminContextPath();
        return "http://localhost:" + getDropwizard().getAdminPort() + adminPath;
    }

    String buildProxyAppPath(final String path) {
        return getProxyBaseAppUrl().replaceAll("/$", "") + path;
    }

    String buildProxyAdminPath(final String path) {
        return getProxyBaseAdminUrl().replaceAll("/$", "") + path;
    }
}
