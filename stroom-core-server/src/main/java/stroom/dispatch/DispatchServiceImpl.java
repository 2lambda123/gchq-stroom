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

package stroom.dispatch;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.dispatch.shared.DispatchService;
import stroom.docref.SharedObject;
import stroom.entity.util.EntityServiceExceptionUtil;
import stroom.event.logging.api.HttpServletRequestHolder;
import stroom.security.SecurityContext;
import stroom.security.util.UserTokenUtil;
import stroom.servlet.SessionListListener;
import stroom.task.api.TaskHandler;
import stroom.task.api.TaskIdFactory;
import stroom.task.api.TaskManager;
import stroom.task.impl.TaskHandlerBeanRegistry;
import stroom.task.shared.Action;
import stroom.util.shared.EntityServiceException;
import stroom.util.shared.ModelStringUtil;
import stroom.util.shared.PermissionException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DispatchServiceImpl extends RemoteServiceServlet implements DispatchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DispatchServiceImpl.class);

    // This path is where we expect to find the `.gwt.rpc` file. It must be valid for startup for both dev and executable
    // jar. If you change where the webapp/ui files are stored, which is currently 'ui', then this path must change too.
    private static final String GWT_RPC_PATH_STROOM = "/ui/stroom/%s.gwt.rpc";
    private static final String GWT_RPC_PATH_DASHBOARD = "/ui/dashboard/%s.gwt.rpc";

    private static final Map<String, String> MODULE_BASE_URL_TO_RPC = new HashMap<>();

    static {
        MODULE_BASE_URL_TO_RPC.put("/stroom/", GWT_RPC_PATH_STROOM);
        MODULE_BASE_URL_TO_RPC.put("/dashboard/", GWT_RPC_PATH_DASHBOARD);
    }

    private final TaskHandlerBeanRegistry taskHandlerBeanRegistry;
    private final TaskManager taskManager;
    private final SecurityContext securityContext;
    private final transient HttpServletRequestHolder httpServletRequestHolder;

    @Inject
    public DispatchServiceImpl(final TaskHandlerBeanRegistry taskHandlerBeanRegistry,
                               final TaskManager taskManager,
                               final SecurityContext securityContext,
                               final HttpServletRequestHolder httpServletRequestHolder) {
        this.taskHandlerBeanRegistry = taskHandlerBeanRegistry;
        this.taskManager = taskManager;
        this.securityContext = securityContext;
        this.httpServletRequestHolder = httpServletRequestHolder;
    }

    @Override
    public <R extends SharedObject> R exec(final Action<R> action) {
        final long startTime = System.currentTimeMillis();

        LOGGER.debug("exec() - >> {} {}", action.getClass().getName(), httpServletRequestHolder);

        final TaskHandler taskHandlerBean = taskHandlerBeanRegistry.findHandler(action);
        if (taskHandlerBean == null) {
            throw new EntityServiceException("No handler for " + action.getClass(), null, false);
        }
        final String userName = securityContext.getUserId();
        // Set the id before we can execute this action.
        action.setId(TaskIdFactory.create());
        action.setUserToken(UserTokenUtil.create(userName, httpServletRequestHolder.getSessionId()));

        try {
            final R r = taskManager.exec(action);
            LOGGER.debug("exec() - >> {} returns {}", action.getClass().getName(), r);
            return r;
        } catch (final PermissionException e) {
            LOGGER.debug(e.getMessage(), e);
            throw new EntityServiceException(e.getGenericMessage());
        } catch (final RuntimeException e) {
            LOGGER.debug(e.getMessage(), e);
            throw EntityServiceExceptionUtil.create(e);
        } finally {
            LOGGER.debug("exec() - << {} took {}", action.getClass().getName(),
                    ModelStringUtil.formatDurationString(System.currentTimeMillis() - startTime));
        }
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            SessionListListener.setLastRequest(req);

            super.service(req, resp);

        } catch (final RuntimeException e) {
            LOGGER.error("handle() - {}", req.getRequestURI(), e);
            throw e;
        }
    }

    /**
     * We need to override this method to customise the location of the `.gwt.rpc` file.
     * Reason: the path is different when the application is bundled as a fat jar.
     */
    @Override
    protected SerializationPolicy doGetSerializationPolicy(
            HttpServletRequest request, String moduleBaseURL, String strongName) {
        final Optional<String> gwtRpcPath = MODULE_BASE_URL_TO_RPC.entrySet().stream()
                .filter(e -> moduleBaseURL.endsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
        LOGGER.debug("Remote Service Handler " + gwtRpcPath);

        if (!gwtRpcPath.isPresent()) {
            final String msg = String.format("Could not find GWT RPC Policy Path for moduleBaseURL: %s", moduleBaseURL);
            throw new RuntimeException(msg);
        }

        final String serializationPolicyFilePath = String.format(gwtRpcPath.get(), strongName);
        try (InputStream is = getClass().getResourceAsStream(serializationPolicyFilePath)) {
            return SerializationPolicyLoader.loadFromStream(is, null);
        } catch (ParseException | IOException | RuntimeException e) {
            LOGGER.debug(e.getMessage(), e);
        }

        return null;
    }
}
