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

package stroom.pipeline.refdata;

import stroom.cache.api.CacheManager;
import stroom.cache.api.ICache;
import stroom.meta.api.EffectiveMetaDataCriteria;
import stroom.meta.api.MetaService;
import stroom.meta.shared.Meta;
import stroom.pipeline.errorhandler.ProcessException;
import stroom.security.api.SecurityContext;
import stroom.util.NullSafe;
import stroom.util.Period;
import stroom.util.shared.Clearable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Singleton;

// TODO: 14/09/2022 Ideally this class ought to listen for events each time a strm is created/deleted
//  or a feed is deleted and then evict the appropriate keys, but that is a LOT of events going over the
//  cluster.
@Singleton
public class EffectiveStreamCache implements Clearable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EffectiveStreamCache.class);

    private static final String CACHE_NAME = "Reference Data - Effective Stream Cache";

    private final ICache<EffectiveStreamKey, NavigableSet<EffectiveStream>> cache;
    private final MetaService metaService;
    private final EffectiveStreamInternPool internPool;
    private final SecurityContext securityContext;

    @Inject
    EffectiveStreamCache(final CacheManager cacheManager,
                         final MetaService metaService,
                         final EffectiveStreamInternPool internPool,
                         final SecurityContext securityContext,
                         final ReferenceDataConfig referenceDataConfig) {
        this.metaService = metaService;
        this.internPool = internPool;
        this.securityContext = securityContext;

        cache = cacheManager.create(CACHE_NAME, referenceDataConfig::getEffectiveStreamCache, this::create);
    }

    public NavigableSet<EffectiveStream> get(final EffectiveStreamKey effectiveStreamKey) {
        if (effectiveStreamKey.getFeed() == null) {
            throw new ProcessException("No feed has been specified for reference data lookup");
        }
        if (effectiveStreamKey.getStreamType() == null) {
            throw new ProcessException("No stream type has been specified for reference data lookup");
        }

        return cache.get(effectiveStreamKey);
    }

    protected NavigableSet<EffectiveStream> create(final EffectiveStreamKey key) {
        return securityContext.asProcessingUserResult(() -> {
            NavigableSet<EffectiveStream> effectiveStreamSet = Collections.emptyNavigableSet();

            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Creating effective time set: " + key.toString());
                }

                // Only find streams for the supplied feed and stream type.
                final EffectiveMetaDataCriteria criteria = new EffectiveMetaDataCriteria();
                criteria.setFeed(key.getFeed());
                criteria.setType(key.getStreamType());

                // Limit the stream set to the requested effective time window.
                final Period window = new Period(key.getFromMs(), key.getToMs());
                criteria.setEffectivePeriod(window);

                // Locate all streams that fit the supplied criteria.
                final Set<Meta> streams = metaService.findEffectiveData(criteria);

                // Add all streams that we have found to the effective stream set.
                if (streams != null && streams.size() > 0) {
                    effectiveStreamSet = new TreeSet<>();
                    for (final Meta meta : streams) {
                        EffectiveStream effectiveStream;

                        if (meta.getEffectiveMs() != null) {
                            effectiveStream = new EffectiveStream(meta.getId(), meta.getEffectiveMs());
                        } else {
                            effectiveStream = new EffectiveStream(meta.getId(), meta.getCreateMs());
                        }

                        addEffectiveStream(effectiveStreamSet, effectiveStream);
                    }
                }

                // Intern the effective stream set so we only have one identical
                // copy in memory.
                if (internPool != null) {
                    effectiveStreamSet = internPool.intern(effectiveStreamSet);
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created effective stream set: " + key.toString());
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }

            return effectiveStreamSet;
        });
    }

    private void addEffectiveStream(final NavigableSet<EffectiveStream> effectiveStreamSet,
                                    final EffectiveStream effectiveStream) {
        final boolean success = effectiveStreamSet.add(effectiveStream);

        // Warn if there are more than one effective stream for
        // exactly the same time.
        if (!success) {
            EffectiveStream existingEffectiveStream = null;
            try {
                // Establish the one we are clashing with
                final NavigableSet<EffectiveStream> subSet = effectiveStreamSet.subSet(
                        effectiveStream,
                        true,
                        effectiveStream,
                        true);
                existingEffectiveStream = subSet.first();
            } catch (Exception e) {
                LOGGER.debug("Error finding existingEffectiveStream, {}", e.getMessage(), e);
            }
            final Long existingStreamId = NullSafe.get(
                    existingEffectiveStream, EffectiveStream::getStreamId);

            LOGGER.warn("Failed attempt to insert effective stream with id="
                    + effectiveStream.getStreamId()
                    + ". Duplicate match found with id=" + existingStreamId
                    + ", effectiveMs=" + effectiveStream.getEffectiveMs()
                    + " (" + Instant.ofEpochMilli(effectiveStream.getEffectiveMs()) + "). "
                    + "You have >1 ref streams with the same effective date, only stream with id="
                    + existingStreamId + " will be used.");
        }
    }

    long size() {
        return cache.size();
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
