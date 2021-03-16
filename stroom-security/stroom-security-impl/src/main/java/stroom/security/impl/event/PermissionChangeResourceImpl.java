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

package stroom.security.impl.event;

import stroom.event.logging.rs.api.AutoLogged;
import stroom.event.logging.rs.api.AutoLogged.OperationType;
import stroom.node.api.NodeService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.client.Entity;

@Singleton
@AutoLogged(OperationType.UNLOGGED)
        //Perm changes logged by DocPermissionResourceImpl
class PermissionChangeResourceImpl implements PermissionChangeResource {

    private final Provider<NodeService> nodeServiceProvider;
    private final Provider<PermissionChangeEventHandlers> permissionChangeEventHandlersProvider;

    @Inject
    PermissionChangeResourceImpl(final Provider<NodeService> nodeServiceProvider,
                                 final Provider<PermissionChangeEventHandlers> permissionChangeEventHandlersProvider) {
        this.nodeServiceProvider = nodeServiceProvider;
        this.permissionChangeEventHandlersProvider = permissionChangeEventHandlersProvider;
    }

    @Override
    public Boolean fireChange(final String nodeName, final PermissionChangeRequest request) {
        final Boolean result = nodeServiceProvider.get().remoteRestResult(
                nodeName,
                Boolean.class,
                () -> {
                    permissionChangeEventHandlersProvider.get().fireLocally(request.getEvent());
                    return true;
                },
                builder ->
                        builder.post(Entity.json(request)));

        return result;
    }
}
