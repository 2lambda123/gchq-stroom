package stroom.proxy.app.handler;

import stroom.meta.api.AttributeMap;
import stroom.meta.api.AttributeMapUtil;
import stroom.meta.api.StandardHeaderArguments;
import stroom.proxy.StroomStatusCode;
import stroom.proxy.app.ReceiveDataConfig;
import stroom.proxy.repo.CSVFormatter;
import stroom.proxy.repo.LogStream;
import stroom.receive.common.AttributeMapFilter;
import stroom.receive.common.AttributeMapValidator;
import stroom.receive.common.ProgressHandler;
import stroom.receive.common.RequestHandler;
import stroom.receive.common.StroomStreamException;
import stroom.receive.common.StroomStreamProcessor;
import stroom.security.api.RequestAuthenticator;
import stroom.security.api.UserIdentity;
import stroom.util.io.ByteCountInputStream;
import stroom.util.io.StreamUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.Metrics;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

/**
 * Main entry point to handling proxy requests.
 * <p>
 * This class used the main context and forwards the request on to our
 * dynamic mini proxy.
 */
public class ProxyRequestHandler implements RequestHandler {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ProxyRequestHandler.class);
    private static final Logger RECEIVE_LOG = LoggerFactory.getLogger("receive");

    private final ReceiveStreamHandlers receiveStreamHandlerProvider;
    private final AttributeMapFilter attributeMapFilter;
    private final LogStream logStream;
    private final Provider<ReceiveDataConfig> receiveDataConfigProvider;
    private final RequestAuthenticator requestAuthenticator;
    private final ProxyId proxyId;

    @Inject
    public ProxyRequestHandler(final ReceiveStreamHandlers receiveStreamHandlerProvider,
                               final AttributeMapFilterFactory attributeMapFilterFactory,
                               final LogStream logStream,
                               final Provider<ReceiveDataConfig> receiveDataConfigProvider,
                               final RequestAuthenticator requestAuthenticator,
                               final ProxyId proxyId) {
        this.receiveStreamHandlerProvider = receiveStreamHandlerProvider;
        this.logStream = logStream;
        attributeMapFilter = attributeMapFilterFactory.create();
        this.receiveDataConfigProvider = receiveDataConfigProvider;
        this.requestAuthenticator = requestAuthenticator;
        this.proxyId = proxyId;
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response) {
        final long startTimeMs = System.currentTimeMillis();
        AttributeMap attributeMap = null;
        try {
            // Create an attribute map from the request.
            attributeMap = AttributeMapUtil.create(request);

            // Create a new proxy id for the stream and report it back to the sender,
            final String proxyIdProperty = addProxyId(attributeMap);

            try {
                // Authenticate the request.
                authenticate(attributeMap, request);
                // Validate the supplied attributes.
                validate(attributeMap);
                // Stream the data.
                stream(startTimeMs, attributeMap, request);

            } finally {
                LOGGER.debug(() -> "Adding proxy id attribute: " + proxyIdProperty);
                try (final PrintWriter writer = response.getWriter()) {
                    writer.println(proxyIdProperty);
                } catch (final IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            response.setStatus(HttpStatus.SC_OK);

        } catch (final Exception e) {
            error(e,
                    startTimeMs,
                    request.getRequestURI(),
                    attributeMap,
                    response);
        }
    }

    private String addProxyId(final AttributeMap attributeMap) {
        final String attributeKey = proxyId.getId();
        final String attributeName = UUID.randomUUID().toString();
        final String proxyIdProperty = attributeKey + ": " + attributeName;
        attributeMap.put(attributeKey, attributeName);
        return proxyIdProperty;
    }

    private void authenticate(final AttributeMap attributeMap,
                              final HttpServletRequest request) {
        final ReceiveDataConfig receiveDataConfig = receiveDataConfigProvider.get();

        final String authorisationHeader = attributeMap.get(HttpHeaders.AUTHORIZATION);

        // Remove authorization header from attributes so we don't persist.
        attributeMap.remove(HttpHeaders.AUTHORIZATION);

        // If token authentication is required but no token is supplied then error.
        if (receiveDataConfig.isRequireTokenAuthentication() &&
                (authorisationHeader == null || authorisationHeader.isBlank())) {
            throw new StroomStreamException(StroomStatusCode.CLIENT_TOKEN_REQUIRED, attributeMap);
        }

        // Authenticate the request token if there is one.
        final Optional<UserIdentity> optionalUserIdentity = requestAuthenticator.authenticate(request);

        // Add the user identified in the token (if present) to the attribute map.
        optionalUserIdentity
                .map(UserIdentity::getId)
                .ifPresent(id -> attributeMap.put("UploadUser", id));

        if (receiveDataConfig.isRequireTokenAuthentication() && optionalUserIdentity.isEmpty()) {
            // If token authentication is required, but we could not verify the token then error.
            throw new StroomStreamException(StroomStatusCode.CLIENT_TOKEN_NOT_AUTHORISED, attributeMap);
        }
    }

    private void validate(final AttributeMap attributeMap) {
        final String feedName = attributeMap.get(StandardHeaderArguments.FEED);
        if (feedName == null || feedName.trim().isEmpty()) {
            throw new StroomStreamException(StroomStatusCode.FEED_MUST_BE_SPECIFIED, attributeMap);
        }

        final ReceiveDataConfig receiveDataConfig = receiveDataConfigProvider.get();
        AttributeMapValidator.validate(
                attributeMap,
                receiveDataConfig::getMetaTypes);
    }

    private void stream(final long startTimeMs,
                        final AttributeMap attributeMap,
                        final HttpServletRequest request) {
        Metrics.measure("ProxyRequestHandler - stream", () -> {
            try (final ByteCountInputStream inputStream =
                    new ByteCountInputStream(request.getInputStream())) {
                // Test to see if we are going to accept this stream or drop the data.
                if (attributeMapFilter.filter(attributeMap)) {
                    // Consume the data
                    Metrics.measure("ProxyRequestHandler - handle", () -> {
                        final String feedName = attributeMap.get(StandardHeaderArguments.FEED);
                        final String typeName = attributeMap.get(StandardHeaderArguments.TYPE);
                        receiveStreamHandlerProvider.handle(feedName, typeName, attributeMap, handler -> {
                            final StroomStreamProcessor stroomStreamProcessor = new StroomStreamProcessor(
                                    attributeMap,
                                    handler,
                                    new ProgressHandler("Receiving data"));
                            stroomStreamProcessor.processRequestHeader(request);
                            stroomStreamProcessor.processInputStream(inputStream, "");
                        });
                    });

                    final long duration = System.currentTimeMillis() - startTimeMs;
                    logStream.log(
                            RECEIVE_LOG,
                            attributeMap,
                            "RECEIVE",
                            request.getRequestURI(),
                            HttpServletResponse.SC_OK,
                            inputStream.getCount(),
                            duration);

                } else {
                    // Just read the stream in and ignore it
                    final byte[] buffer = new byte[StreamUtil.BUFFER_SIZE];
                    while (inputStream.read(buffer) >= 0) {
                        // Ignore data.
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(new String(buffer));
                        }
                    }
                    LOGGER.warn("\"Dropped stream\",{}", CSVFormatter.format(attributeMap));

                    final long duration = System.currentTimeMillis() - startTimeMs;
                    logStream.log(
                            RECEIVE_LOG,
                            attributeMap,
                            "DROP",
                            request.getRequestURI(),
                            HttpServletResponse.SC_OK,
                            inputStream.getCount(),
                            duration);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void error(final Exception e,
                       final long startTimeMs,
                       final String requestUri,
                       final AttributeMap attributeMap,
                       final HttpServletResponse response) {
        LOGGER.error(() -> (e.getMessage() == null
                ? e.getClass().getName()
                : e.getMessage()) +
                "\n" +
                CSVFormatter.format(attributeMap), e);

        final RuntimeException unwrappedException = StroomStreamException.unwrap(e, attributeMap);
        StroomStatusCode stroomStatusCode = StroomStatusCode.UNKNOWN_ERROR;
        final String message = StroomStreamException.unwrapMessage(unwrappedException);

        if (unwrappedException instanceof StroomStreamException) {
            stroomStatusCode = ((StroomStreamException) unwrappedException).getStroomStatusCode();
        }

        if (stroomStatusCode == null) {
            stroomStatusCode = StroomStatusCode.UNKNOWN_ERROR;
        }

        final StroomStatusCode finalStroomStatusCode = stroomStatusCode;
        LOGGER.warn(() -> {
            final StringBuilder clientDetailsStringBuilder = new StringBuilder();
            AttributeMapUtil.appendAttributes(
                    attributeMap,
                    clientDetailsStringBuilder,
                    StandardHeaderArguments.X_FORWARDED_FOR,
                    StandardHeaderArguments.REMOTE_HOST,
                    StandardHeaderArguments.REMOTE_ADDRESS,
                    StandardHeaderArguments.RECEIVED_PATH);

            final String clientDetailsStr = clientDetailsStringBuilder.isEmpty()
                    ? ""
                    : " - " + clientDetailsStringBuilder;

            return "Sending error response "
                    + finalStroomStatusCode.getHttpCode()
                    + " - "
                    + message
                    + clientDetailsStr;
        });

        final int returnCode = finalStroomStatusCode.getCode();
        final long duration = System.currentTimeMillis() - startTimeMs;

        if (StroomStatusCode.FEED_IS_NOT_SET_TO_RECEIVED_DATA.equals(finalStroomStatusCode)) {
            logStream.log(
                    RECEIVE_LOG,
                    attributeMap,
                    "REJECT",
                    requestUri,
                    returnCode,
                    -1,
                    duration);
        } else {
            logStream.log(
                    RECEIVE_LOG,
                    attributeMap,
                    "ERROR",
                    requestUri,
                    returnCode,
                    -1,
                    duration);
        }

        response.setHeader(StandardHeaderArguments.STROOM_STATUS,
                String.valueOf(stroomStatusCode.getCode()));
        try {
            response.sendError(stroomStatusCode.getHttpCode(), message);
        } catch (final Throwable e2) {
            LOGGER.debug(e2::getMessage, e2);
        }
    }
}
