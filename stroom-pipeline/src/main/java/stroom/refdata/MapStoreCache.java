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

package stroom.refdata;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import stroom.node.server.StroomPropertyService;
import stroom.security.SecurityHelper;
import stroom.security.SecurityContext;
import stroom.util.cache.CacheManager;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Implementation class that stores reference data from reference data feeds.
 * </p>
 */
@Component
public final class MapStoreCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapStoreCache.class);

    private static final String MAXIMUM_SIZE_PROPERTY = "stroom.referenceData.mapStore.maxCacheSize";
    private static final long DEFAULT_MAXIMUM_SIZE = 100;

    private final LoadingCache<MapStoreCacheKey, MapStore> cache;
    private final ReferenceDataLoader referenceDataLoader;
    private final MapStoreInternPool internPool;
    private final SecurityContext securityContext;

    @Inject
    @SuppressWarnings("unchecked")
    MapStoreCache(final CacheManager cacheManager,
                  final ReferenceDataLoader referenceDataLoader,
                  final MapStoreInternPool internPool,
                  final SecurityContext securityContext,
                  final StroomPropertyService stroomPropertyService) {
        this.referenceDataLoader = referenceDataLoader;
        this.internPool = internPool;
        this.securityContext = securityContext;

        final long maximumSize = stroomPropertyService.getLongProperty(MAXIMUM_SIZE_PROPERTY, DEFAULT_MAXIMUM_SIZE);

        final CacheLoader<MapStoreCacheKey, MapStore> cacheLoader = CacheLoader.from(this::create);
        final CacheBuilder cacheBuilder = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterAccess(1, TimeUnit.HOURS);
        cache = cacheBuilder.build(cacheLoader);
        cacheManager.registerCache("Reference Data - Map Store Cache", cacheBuilder, cache);
    }

    public MapStore get(final MapStoreCacheKey key) {
        return cache.getUnchecked(key);
    }

    private MapStore create(final MapStoreCacheKey mapStoreCacheKey) {
        try (SecurityHelper securityHelper = SecurityHelper.processingUser(securityContext)) {
            MapStore mapStore = null;

            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Creating reference data map store: " + mapStoreCacheKey.toString());
                }

                // Load the data into the map store.
                mapStore = referenceDataLoader.load(mapStoreCacheKey);
                // Intern the map store so we only have one identical copy in
                // memory.
                if (internPool != null) {
                    mapStore = internPool.intern(mapStore);
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created reference data map store: " + mapStoreCacheKey.toString());
                }
            } catch (final Throwable e) {
                LOGGER.error(e.getMessage(), e);
            }

            // Make sure this pool always returns some kind of map store even if an
            // exception was thrown during load.
            if (mapStore == null) {
                mapStore = new MapStoreImpl();
            }

            return mapStore;
        }
    }
}
