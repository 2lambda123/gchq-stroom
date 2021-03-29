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

package stroom.security.impl.session;

import stroom.security.api.SecurityContext;
import stroom.util.date.DateUtil;
import stroom.util.shared.IsServlet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class SessionListServlet extends HttpServlet implements IsServlet {

    private static final long serialVersionUID = 8723931558071593017L;

    private static final Set<String> PATH_SPECS = Set.of("/sessionList");

    private final SecurityContext securityContext;
    private final SessionListService sessionListService;

    @Inject
    SessionListServlet(final SecurityContext securityContext,
                       final SessionListService sessionListService) {
        this.securityContext = securityContext;
        this.sessionListService = sessionListService;
    }

    /**
     * Method interceptor needs to go on public API By-pass authentication / authorisation checks.
     * <p>
     * This servlet is NOT protected by default and should be filtered by Apache access controls, see documentation for
     * details.
     */
    @Override
    public void service(final ServletRequest req, final ServletResponse res) {
        // TODO not sure this should be here as sessionList should be an authenticated servlet
        securityContext.insecure(() -> {
            try {
                super.service(req, res);
            } catch (ServletException e) {
                throw new RuntimeException(e.getMessage(), e);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        response.setContentType("text/html");

        final List<List<String>> table = new ArrayList<>();

        sessionListService.listSessions().forEach(sessionDetails -> {
            final ArrayList<String> row = new ArrayList<>();
            row.add(DateUtil.createNormalDateTimeString(sessionDetails.getLastAccessedMs()));
            row.add(DateUtil.createNormalDateTimeString(sessionDetails.getCreateMs()));
            row.add(sessionDetails.getUserName());
            row.add(sessionDetails.getNodeName());
            row.add("<span class=\"agent\">" + sessionDetails.getLastAccessedAgent() + "</span>");
            table.add(row);
        });

        table.sort((l1, l2) -> l2.get(0).compareTo(l1.get(0)));

        response.getWriter().write("<html>" +
                "<head><link type=\"text/css\" href=\"css/SessionList.css\" rel=\"stylesheet\" /></head>" +
                "<body>");
        response.getWriter().write("<table>");
        response.getWriter().write("<thead>" +
                "<tr>" +
                "<th>Last Accessed</th>" +
                "<th>Created</th>" +
                "<th>User Id</th>" +
                "<th>Node</th>" +
                "<th>Agent</th>" +
                "</tr>" +
                "</thead>");

        for (final List<String> row : table) {
            response.getWriter().write("<tr>");

            for (final String cell : row) {
                response.getWriter().write("<td>");
                if (cell == null) {
                    response.getWriter().write("-");
                } else {
                    response.getWriter().write(cell);
                }
                response.getWriter().write("</td>");
            }
            response.getWriter().write("</tr>");
        }
        response.getWriter().write("</table>");
        response.getWriter().write("</body></html>");

        response.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * @return The part of the path that will be in addition to any base path,
     * e.g. "/datafeed".
     */
    @Override
    public Set<String> getPathSpecs() {
        return PATH_SPECS;
    }
}
