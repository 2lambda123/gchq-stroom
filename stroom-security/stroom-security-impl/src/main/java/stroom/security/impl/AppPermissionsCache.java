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

package stroom.security.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import stroom.cache.api.CacheManager;
import stroom.cache.api.CacheUtil;
import stroom.security.shared.PermissionNames;
import stroom.security.shared.User;
import stroom.security.shared.UserAppPermissions;
import stroom.util.shared.Clearable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
// TODO watch for changes somehow, it used to use the generic entity event handler stuff
public class AppPermissionsCache implements Clearable {
    private static final int MAX_CACHE_ENTRIES = 1000;

    private final AppPermissionDao appPermissionDao;
    private final LoadingCache<User, UserAppPermissions> cache;

    @Inject
    @SuppressWarnings("unchecked")
    AppPermissionsCache(final CacheManager cacheManager,
                        final AppPermissionDao appPermissionDao) {
        this.appPermissionDao = appPermissionDao;
        final CacheLoader<User, UserAppPermissions> cacheLoader = CacheLoader.from(this::getPermissionsForUser);
        final CacheBuilder cacheBuilder = CacheBuilder.newBuilder()
                .maximumSize(MAX_CACHE_ENTRIES)
                .expireAfterAccess(30, TimeUnit.MINUTES);
        cache = cacheBuilder.build(cacheLoader);
        cacheManager.registerCache("User App Permissions Cache", cacheBuilder, cache);
    }

    UserAppPermissions get(final User key) {
        return cache.getUnchecked(key);
    }

    void remove(final User userRef) {
        cache.invalidate(userRef);
    }

    @Override
    public void clear() {
        CacheUtil.clear(cache);
    }

    private UserAppPermissions getPermissionsForUser(User userRef) {
        final Set<String> permissionNames = appPermissionDao.getPermissionsForUser(userRef.getUuid());
        final Set<String> allNames = PermissionNames.ALL_PERMISSIONS;

        return new UserAppPermissions(userRef, allNames, permissionNames);
    }
}
