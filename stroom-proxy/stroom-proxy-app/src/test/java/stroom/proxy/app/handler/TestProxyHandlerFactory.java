package stroom.proxy.app.handler;

import stroom.proxy.repo.ProxyRepositoryConfig;
import stroom.proxy.repo.ProxyRepositoryManager;
import stroom.proxy.repo.ProxyRepositoryStreamHandler;
import stroom.proxy.repo.ProxyRepositoryStreamHandlerFactory;
import stroom.proxy.repo.StreamHandler;
import stroom.test.common.util.test.StroomUnitTest;
import stroom.util.io.FileUtil;
import stroom.util.io.PathCreator;
import stroom.util.shared.BuildInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Provider;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestProxyHandlerFactory extends StroomUnitTest {

    @Test
    void testStoreAndForward(@TempDir final Path tempDir) {
        final MasterStreamHandlerFactory proxyHandlerFactory = getProxyHandlerFactory(tempDir, true, true);
        final List<StreamHandler> incomingHandlers = proxyHandlerFactory.addReceiveHandlers(new ArrayList<>());

        assertThat(incomingHandlers.size() == 1 && incomingHandlers.get(0) instanceof ProxyRepositoryStreamHandler).as(
                "Expecting 1 handler that saves to the repository").isTrue();

        final List<StreamHandler> outgoingHandlers = proxyHandlerFactory.addSendHandlers(new ArrayList<>());
        assertThat(outgoingHandlers.size() == 2 && outgoingHandlers.get(0) instanceof ForwardStreamHandler
                && outgoingHandlers.get(1) instanceof ForwardStreamHandler).as(
                "Expecting 2 handler that forward to other URLS").isTrue();
    }

    @Test
    void testForward(@TempDir final Path tempDir) {
        final MasterStreamHandlerFactory proxyHandlerFactory = getProxyHandlerFactory(tempDir, false, true);

        for (int i = 0; i < 2; i++) {
            final List<StreamHandler> incomingHandlers = proxyHandlerFactory.addReceiveHandlers(new ArrayList<>());
            assertThat(incomingHandlers.size() == 2 && incomingHandlers.get(0) instanceof ForwardStreamHandler
                    && incomingHandlers.get(1) instanceof ForwardStreamHandler).as(
                    "Expecting 2 handler that forward to other URLS").isTrue();

            assertThat(((ForwardStreamHandler) incomingHandlers.get(0)).getForwardUrl()).isEqualTo("https://url1");
            assertThat(((ForwardStreamHandler) incomingHandlers.get(1)).getForwardUrl()).isEqualTo("https://url2");

            final List<StreamHandler> outgoingHandlers = proxyHandlerFactory.addSendHandlers(new ArrayList<>());
            assertThat(outgoingHandlers.size() == 0).as("Expecting 0 handler that forward to other URLS").isTrue();
        }
    }

    @Test
    void testStore(@TempDir final Path tempDir) {
        final MasterStreamHandlerFactory proxyHandlerFactory = getProxyHandlerFactory(tempDir, true, false);

        final List<StreamHandler> incomingHandlers = proxyHandlerFactory.addReceiveHandlers(new ArrayList<>());
        assertThat(incomingHandlers.size() == 1 && incomingHandlers.get(0) instanceof ProxyRepositoryStreamHandler).as(
                "Expecting 1 handler that stores incoming data").isTrue();

        final List<StreamHandler> outgoingHandlers = proxyHandlerFactory.addSendHandlers(new ArrayList<>());
        assertThat(outgoingHandlers.size() == 0).as("Expecting 1 handlers that forward to other URLS").isTrue();
    }

    private MasterStreamHandlerFactory getProxyHandlerFactory(final Path tempDir,
                                                              final boolean isStoringEnabled,
                                                              final boolean isForwardingenabled) {
        final LogStreamConfig logRequestConfig = null;
        final ProxyRepositoryConfig proxyRepositoryConfig = new ProxyRepositoryConfig()
                .withRepoDir(FileUtil.getCanonicalPath(getCurrentTestDir()))
                .withStoringEnabled(isStoringEnabled);

        ForwardDestinationConfig destinationConfig1 = new ForwardDestinationConfig()
                .withForwardUrl("https://url1");

        ForwardDestinationConfig destinationConfig2 = new ForwardDestinationConfig()
                .withForwardUrl("https://url2");

        final ForwardStreamConfig forwardRequestConfig = new ForwardStreamConfig()
                .withForwardingEnabled(isForwardingenabled)
                .withForwardDestinations(List.of(destinationConfig1, destinationConfig2));

        final PathCreator pathCreator = new PathCreator(
                () -> tempDir.resolve("home"),
                () -> tempDir);

        final ProxyRepositoryManager proxyRepositoryManager = new ProxyRepositoryManager(
                () -> tempDir,
                proxyRepositoryConfig,
                pathCreator);
        final Provider<ProxyRepositoryStreamHandler> proxyRepositoryRequestHandlerProvider = () ->
                new ProxyRepositoryStreamHandler(proxyRepositoryManager);

        final LogStream logStream = new LogStream(logRequestConfig);
        final ProxyRepositoryStreamHandlerFactory proxyRepositoryStreamHandlerFactory =
                new ProxyRepositoryStreamHandlerFactory(proxyRepositoryConfig, proxyRepositoryRequestHandlerProvider);


        final BuildInfo buildInfo = new BuildInfo("now", "test version", "now");
        final ForwardStreamHandlerFactory forwardStreamHandlerFactory = new ForwardStreamHandlerFactory(
                logStream, forwardRequestConfig, proxyRepositoryConfig, () -> buildInfo, pathCreator);

        return new MasterStreamHandlerFactory(proxyRepositoryStreamHandlerFactory, forwardStreamHandlerFactory);
    }
}
