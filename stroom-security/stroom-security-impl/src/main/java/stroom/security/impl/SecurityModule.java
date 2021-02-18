/*
 * Copyright 2018 Crown Copyright
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

import stroom.security.api.DocumentPermissionService;
import stroom.security.api.TokenVerifier;
import stroom.security.impl.event.PermissionChangeEvent;
import stroom.security.impl.event.PermissionChangeEventLifecycleModule;
import stroom.security.impl.event.PermissionChangeEventModule;
import stroom.util.entityevent.EntityEvent;
import stroom.util.guice.FilterBinder;
import stroom.util.guice.FilterInfo;
import stroom.util.guice.GuiceUtil;
import stroom.util.guice.RestResourcesBinder;
import stroom.util.shared.Clearable;

import com.google.inject.AbstractModule;
import org.apache.http.impl.client.CloseableHttpClient;

import javax.servlet.http.HttpSessionListener;

public class SecurityModule extends AbstractModule {
    private static final String MATCH_ALL_PATHS = "/*";

    @Override
    protected void configure() {
        install(new PermissionChangeEventModule());
        install(new PermissionChangeEventLifecycleModule());

        bind(UserAppPermissionService.class).to(UserAppPermissionServiceImpl.class);
        bind(DocumentPermissionService.class).to(DocumentPermissionServiceImpl.class);
        bind(UserService.class).to(UserServiceImpl.class);
        bind(TokenVerifier.class).to(TokenVerifierImpl.class);
        bind(JwtContextFactory.class).to(JwtContextFactoryImpl.class);
        bind(CloseableHttpClient.class).toProvider(HttpClientProvider.class);

        FilterBinder.create(binder())
                .bind(new FilterInfo(ContentSecurityFilter.class.getSimpleName(), MATCH_ALL_PATHS),
                        ContentSecurityFilter.class)
                .bind(new FilterInfo(SecurityFilter.class.getSimpleName(), MATCH_ALL_PATHS),
                        SecurityFilter.class);

        // Provide object info to the logging service.
        GuiceUtil.buildMultiBinder(binder(), Clearable.class)
                .addBinding(UserDocumentPermissionsCache.class)
                .addBinding(UserAppPermissionsCache.class)
                .addBinding(UserGroupsCache.class)
                .addBinding(UserCache.class);

        GuiceUtil.buildMultiBinder(binder(), HttpSessionListener.class)
                .addBinding(SessionMap.class);

        GuiceUtil.buildMultiBinder(binder(), EntityEvent.Handler.class)
                .addBinding(UserGroupsCache.class)
                .addBinding(UserAppPermissionsCache.class);

        GuiceUtil.buildMultiBinder(binder(), PermissionChangeEvent.Handler.class)
                .addBinding(UserDocumentPermissionsCache.class);

        RestResourcesBinder.create(binder())
                .bind(AppPermissionResourceImpl.class)
                .bind(StroomSessionResourceImpl.class)
                .bind(DocPermissionResourceImpl.class)
                .bind(SessionResourceImpl.class)
                .bind(UserResourceImpl.class);
    }
}
