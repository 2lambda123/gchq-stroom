/*
 * Copyright 2017 Crown Copyright
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
 *
 */

package stroom.core.receive;

import stroom.data.store.api.Store;
import stroom.feed.api.FeedProperties;
import stroom.meta.api.AttributeMap;
import stroom.meta.api.AttributeMapUtil;
import stroom.meta.api.MetaService;
import stroom.meta.api.StandardHeaderArguments;
import stroom.meta.statistics.api.MetaStatistics;
import stroom.proxy.StroomStatusCode;
import stroom.receive.common.AttributeMapFilter;
import stroom.receive.common.RequestHandler;
import stroom.receive.common.StreamTargetStroomStreamHandler;
import stroom.receive.common.StroomStreamException;
import stroom.receive.common.StroomStreamProcessor;
import stroom.security.api.SecurityContext;
import stroom.util.io.BufferFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Handle the incoming requests and stream them to disk checking a few things.
 * </p>
 */
class ReceiveDataRequestHandler implements RequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveDataRequestHandler.class);

    private final SecurityContext securityContext;
    private final Store streamStore;
    private final FeedProperties feedProperties;
    private final MetaStatistics metaDataStatistics;
    private final AttributeMapFilterFactory attributeMapFilterFactory;
    private final BufferFactory bufferFactory;
    private final MetaService metaService;

    @Inject
    public ReceiveDataRequestHandler(final SecurityContext securityContext,
                                     final Store streamStore,
                                     final FeedProperties feedProperties,
                                     final MetaStatistics metaDataStatistics,
                                     final AttributeMapFilterFactory attributeMapFilterFactory,
                                     final BufferFactory bufferFactory,
                                     final MetaService metaService) {
        this.securityContext = securityContext;
        this.streamStore = streamStore;
        this.feedProperties = feedProperties;
        this.metaDataStatistics = metaDataStatistics;
        this.attributeMapFilterFactory = attributeMapFilterFactory;
        this.bufferFactory = bufferFactory;
        this.metaService = metaService;
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response) {
        securityContext.asProcessingUser(() -> {
            final AttributeMapFilter attributeMapFilter = attributeMapFilterFactory.create();

            final AttributeMap attributeMap = AttributeMapUtil.create(request);
            if (attributeMapFilter.filter(attributeMap)) {
                debug("Receiving data", attributeMap);

                final String feedName = Optional.ofNullable(attributeMap.get(StandardHeaderArguments.FEED))
                        .map(String::trim)
                        .orElse("");
                if (feedName.isEmpty()) {
                    throw new StroomStreamException(StroomStatusCode.FEED_MUST_BE_SPECIFIED);
                }

                // Get the type name from the header arguments if supplied.
                String typeName = Optional.ofNullable(attributeMap.get(StandardHeaderArguments.TYPE))
                        .map(String::trim)
                        .orElse("");
                if (typeName.isEmpty()) {
                    // If no type name is supplied then get the default for the feed.
                    typeName = feedProperties.getStreamTypeName(feedName);
                }

                // Validate the data type name.
                if (!metaService.getTypes().contains(typeName)) {
                    throw new StroomStreamException(StroomStatusCode.UNEXPECTED_DATA_TYPE);
                }

                List<StreamTargetStroomStreamHandler> handlers = StreamTargetStroomStreamHandler
                        .buildSingleHandlerList(
                                streamStore,
                                feedProperties,
                                metaDataStatistics,
                                feedName,
                                typeName);

                final byte[] buffer = bufferFactory.create();
                final StroomStreamProcessor stroomStreamProcessor = new StroomStreamProcessor(
                        attributeMap,
                        handlers,
                        buffer,
                        "DataFeedRequestHandler-" + attributeMap.get(StandardHeaderArguments.GUID));

                try {
                    stroomStreamProcessor.processRequestHeader(request);
                    stroomStreamProcessor.process(getInputStream(request), "");
                    stroomStreamProcessor.closeHandlers();
                    handlers = null;
                } finally {
                    // some kind of error
                    if (handlers != null) {
                        handlers.get(0).closeDelete();
                    }
                }
            } else {
                // Drop the data.
                debug("Dropping data", attributeMap);
            }

            // Set the response status.
            response.setStatus(StroomStatusCode.OK.getHttpCode());
            LOGGER.info("handleRequest response " + StroomStatusCode.OK);
        });
    }

    private void debug(final String message, final AttributeMap attributeMap) {
        if (LOGGER.isDebugEnabled()) {
            final List<String> keys = attributeMap.keySet().stream().sorted().collect(Collectors.toList());
            final StringBuilder sb = new StringBuilder();
            keys.forEach(key -> {
                sb.append(key);
                sb.append("=");
                sb.append(attributeMap.get(key));
                sb.append(",");
            });
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }

            LOGGER.debug(message + " (" + sb.toString() + ")");
        }
    }

    private InputStream getInputStream(final HttpServletRequest request) {
        try {
            return request.getInputStream();
        } catch (final IOException ioEx) {
            throw new StroomStreamException(StroomStatusCode.UNKNOWN_ERROR, ioEx.getMessage());
        }
    }
}
