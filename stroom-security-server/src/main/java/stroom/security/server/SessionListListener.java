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
 */

package stroom.security.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;
import stroom.entity.shared.BaseCriteria;
import stroom.entity.shared.BaseResultList;
import stroom.feed.server.UserAgentSessionUtil;
import stroom.security.ProcessingUserIdentity;
import stroom.security.shared.UserIdentity;
import stroom.servlet.SessionDetails;
import stroom.servlet.SessionListService;
import stroom.task.server.ExtendedFindTaskCriteria;
import stroom.task.server.TaskManager;
import stroom.task.shared.TerminateTaskProgressAction;
import stroom.util.spring.StroomBeanStore;
import stroom.util.task.TaskIdFactory;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * There are 2 instances of this class as spring has no HttpSessionListener
 * functionality so we use 2 instances and some statics
 */
@Component
public class SessionListListener implements HttpSessionListener, SessionListService, BeanFactoryAware {
    private static final ConcurrentHashMap<String, HttpSession> sessionMap = new ConcurrentHashMap<>();
    private static transient Logger logger;

    private static transient volatile BeanFactory beanFactory;

    public static HttpSession getSession(final String sessionId) {
        return sessionMap.get(sessionId);
    }

    private Logger getLogger() {
        // Lazy load the logger.
        if (logger == null) {
            synchronized (SessionListListener.class) {
                if (logger == null) {
                    logger = LoggerFactory.getLogger(SessionListListener.class);
                }
            }
        }

        return logger;
    }

    @Override
    public void sessionCreated(final HttpSessionEvent event) {
        final HttpSession httpSession = event.getSession();
        getLogger().info("sessionCreated() - {}", httpSession.getId());
        sessionMap.put(httpSession.getId(), httpSession);
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent event) {
        final HttpSession httpSession = event.getSession();
        getLogger().info("sessionDestroyed() - {}", httpSession.getId());
        sessionMap.remove(httpSession.getId());

        try {
            // Manually set the id as we are invoking a UI Action Task
            // directly
            final String sessionId = event.getSession().getId();
            final ExtendedFindTaskCriteria criteria = new ExtendedFindTaskCriteria();
            criteria.setSessionId(sessionId);
            final TerminateTaskProgressAction action = new TerminateTaskProgressAction(
                    "Terminate session: " + sessionId, criteria, false);
            action.setUserIdentity(ProcessingUserIdentity.INSTANCE);
            action.setId(TaskIdFactory.create());

            final TaskManager taskManager = getTaskManager();
            if (taskManager != null) {
                taskManager.exec(action);
            }
        } catch (final Exception ex) {
            getLogger().error("sessionDestroyed()", ex);
        }
    }

    @Override
    public BaseResultList<SessionDetails> find(final BaseCriteria criteria) {
        final ArrayList<SessionDetails> rtn = new ArrayList<>();
        for (final HttpSession httpSession : sessionMap.values()) {
            final SessionDetails sessionDetails = new SessionDetails();

            final UserIdentity userIdentity = UserIdentitySessionUtil.get(httpSession);
            if (userIdentity != null) {
                sessionDetails.setUserName(userIdentity.getId());
            }

            sessionDetails.setCreateMs(httpSession.getCreationTime());
            sessionDetails.setLastAccessedMs(httpSession.getLastAccessedTime());
            sessionDetails.setLastAccessedAgent(UserAgentSessionUtil.get(httpSession));

            rtn.add(sessionDetails);
        }
        return BaseResultList.createUnboundedList(rtn);
    }

    @Override
    public BaseCriteria createCriteria() {
        return null;
    }

    private TaskManager getTaskManager() {
        if (beanFactory != null) {
            final StroomBeanStore stroomBeanStore = beanFactory.getBean(StroomBeanStore.class);
            if (stroomBeanStore != null) {
                return stroomBeanStore.getBean(TaskManager.class);
            }
        }

        return null;
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        synchronized (SessionListListener.class) {
            if (beanFactory != null) {
                SessionListListener.beanFactory = beanFactory;
            }
        }
    }
}
