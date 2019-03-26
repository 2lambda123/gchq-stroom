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

package stroom.security.impl;

import stroom.event.logging.api.HttpServletRequestHolder;
import stroom.security.api.Security;
import stroom.security.shared.LogoutAction;
import stroom.security.shared.UserRef;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.shared.VoidResult;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

class LogoutHandler extends AbstractTaskHandler<LogoutAction, VoidResult> {
    private final HttpServletRequestHolder httpServletRequestHolder;
    private final AuthenticationEventLog eventLog;
    private final Security security;

    @Inject
    LogoutHandler(final HttpServletRequestHolder httpServletRequestHolder,
                  final AuthenticationEventLog eventLog,
                  final Security security) {
        this.httpServletRequestHolder = httpServletRequestHolder;
        this.eventLog = eventLog;
        this.security = security;
    }

    @Override
    public VoidResult exec(final LogoutAction task) {
        return security.insecureResult(() -> {
            final HttpSession session = httpServletRequestHolder.get().getSession();
            final UserRef userRef = UserRefSessionUtil.get(session);
            if (session != null) {
                // Invalidate the current user session
                session.invalidate();
            }
            if (userRef != null) {
                // Create an event for logout
                eventLog.logoff(userRef.getName());
            }

            return new VoidResult();
        });
    }
}
