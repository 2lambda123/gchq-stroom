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

package stroom.core.servlet;

import stroom.ui.config.shared.UiConfig;
import stroom.util.io.CloseableUtil;
import stroom.util.io.StreamUtil;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public abstract class AppServlet extends HttpServlet {

    private static final String TITLE = "@TITLE@";
    private static final String ON_CONTEXT_MENU = "@ON_CONTEXT_MENU@";
    private static final String SCRIPT = "@SCRIPT@";

    private final UiConfig uiConfig;

    private String template;

    AppServlet(final UiConfig uiConfig) {
        this.uiConfig = uiConfig;
    }

    private String getHtmlTemplate() {
        if (template == null) {
            final InputStream is = getClass().getResourceAsStream("app.html");
            template = StreamUtil.streamToString(is);
            CloseableUtil.closeLogAndIgnoreException(is);
        }
        return template;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final PrintWriter pw = response.getWriter();
        response.setContentType("text/html");

        String html = getHtmlTemplate();
        html = html.replace(TITLE, uiConfig.getHtmlTitle());
        html = html.replace(ON_CONTEXT_MENU, uiConfig.getOncontextmenu());
        html = html.replace(SCRIPT, getScript());

        pw.write(html);
        pw.close();
    }

    abstract String getScript();
}
