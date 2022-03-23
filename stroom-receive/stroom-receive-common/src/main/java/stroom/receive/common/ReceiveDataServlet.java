/*
 * Copyright 2016 Crown Copyright
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
 */

package stroom.receive.common;

import stroom.util.io.ByteCountInputStream;
import stroom.util.io.StreamUtil;
import stroom.util.shared.IsServlet;
import stroom.util.shared.Unauthenticated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Servlet that streams files to disk based on meta input arguments.
 * </p>
 */
@Unauthenticated
@Singleton
public class ReceiveDataServlet extends HttpServlet implements IsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveDataServlet.class);

    private static final Set<String> PATH_SPECS = Set.of("/datafeed", "/datafeed/*");

    private final Provider<RequestHandler> requestHandlerProvider;

    @Inject
    ReceiveDataServlet(final Provider<RequestHandler> requestHandlerProvider) {
        this.requestHandlerProvider = requestHandlerProvider;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
     */
    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        try {
            try (final ByteCountInputStream inputStream = new ByteCountInputStream(request.getInputStream())) {
                StreamUtil.streamToString(inputStream);
            }
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

//        handleRequest(request, response);
    }

    /**
     * @see HttpServlet#doPut(HttpServletRequest, HttpServletResponse)
     */
    @Override
    protected void doPut(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response);
    }

    /**
     * Do handle the request.
     */
    private void handleRequest(final HttpServletRequest request, final HttpServletResponse response) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getRequestTrace(request));
        }

        try {
            final RequestHandler requestHandler = requestHandlerProvider.get();
            requestHandler.handle(request, response);
        } catch (final RuntimeException e) {
            StroomStreamException.sendErrorResponse(request, response, e);
        }
    }

    /**
     * Utility to log out some trace info.
     */
    private String getRequestTrace(final HttpServletRequest request) {
        final StringBuilder trace = new StringBuilder();
        trace.append("request.getAuthType()=");
        trace.append(request.getAuthType());
        trace.append("\n");
        trace.append("request.getProtocol()=");
        trace.append(request.getProtocol());
        trace.append("\n");
        trace.append("request.getScheme()=");
        trace.append(request.getScheme());
        trace.append("\n");
        trace.append("request.getQueryString()=");
        trace.append(request.getQueryString());
        trace.append("\n");

        final Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            trace.append("request.getHeader('");
            trace.append(header);
            trace.append("')='");
            trace.append(request.getHeader(header));
            trace.append("'\n");
        }

        final Enumeration<String> attributes = request.getAttributeNames();
        while (attributes.hasMoreElements()) {
            String attr = attributes.nextElement();
            trace.append("request.getAttribute('");
            trace.append(attr);
            trace.append("')='");
            trace.append(request.getAttribute(attr));
            trace.append("'\n");
        }

        trace.append("request.getRequestURI()=");
        trace.append(request.getRequestURI());

        return trace.toString();
    }

    @Override
    public Set<String> getPathSpecs() {
        return PATH_SPECS;
    }
}
