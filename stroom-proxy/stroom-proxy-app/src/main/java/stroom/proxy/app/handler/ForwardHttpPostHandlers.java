package stroom.proxy.app.handler;

import stroom.meta.api.AttributeMap;
import stroom.meta.api.AttributeMapUtil;
import stroom.proxy.StroomStatusCode;
import stroom.proxy.app.forwarder.ForwardHttpPostConfig;
import stroom.proxy.repo.LogStream;
import stroom.receive.common.StreamHandler;
import stroom.receive.common.StreamHandlers;
import stroom.receive.common.StroomStreamException;
import stroom.security.api.UserIdentityFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.function.Consumer;
import javax.net.ssl.SSLSocketFactory;

public class ForwardHttpPostHandlers implements StreamHandlers {

    private final LogStream logStream;
    private final String userAgentString;
    private final ForwardHttpPostConfig config;
    private final SSLSocketFactory sslSocketFactory;
    private final UserIdentityFactory userIdentityFactory;

    public ForwardHttpPostHandlers(final LogStream logStream,
                                   final ForwardHttpPostConfig config,
                                   final String userAgentString,
                                   final SSLSocketFactory sslSocketFactory,
                                   final UserIdentityFactory userIdentityFactory) {
        this.logStream = logStream;
        this.userAgentString = userAgentString;
        this.sslSocketFactory = sslSocketFactory;
        this.config = config;
        this.userIdentityFactory = userIdentityFactory;
    }

    @Override
    public void handle(final String feedName,
                       final String typeName,
                       final AttributeMap attributeMap,
                       final Consumer<StreamHandler> consumer) {
        if (feedName.isEmpty()) {
            throw new StroomStreamException(StroomStatusCode.FEED_MUST_BE_SPECIFIED, attributeMap);
        }
        AttributeMapUtil.addFeedAndType(attributeMap, feedName, typeName);

        // We need to add the authentication token to our headers
        final Map<String, String> authHeaders = userIdentityFactory.getServiceUserAuthHeaders();
        attributeMap.putAll(authHeaders);

        HttpSender streamHandler = null;
        try {
            streamHandler = new HttpSender(
                    logStream,
                    config,
                    sslSocketFactory,
                    userAgentString,
                    attributeMap,
                    userIdentityFactory);

            consumer.accept(streamHandler);
            streamHandler.close();
        } catch (final RuntimeException e) {
            if (streamHandler != null) {
                streamHandler.error();
            }
            throw e;
        } catch (final IOException e) {
            if (streamHandler != null) {
                streamHandler.error();
            }
            throw new UncheckedIOException(e);
        }
    }

    ForwardHttpPostConfig getConfig() {
        return config;
    }

    SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }
}
