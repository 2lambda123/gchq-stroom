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

package stroom.feed.impl;

import stroom.cache.api.CacheManager;
import stroom.cache.api.ICache;
import stroom.docref.DocRef;
import stroom.feed.api.FeedStore;
import stroom.feed.shared.FeedDoc;
import stroom.security.api.SecurityContext;
import stroom.util.shared.Clearable;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FeedDocCache implements Clearable {
    private static final String CACHE_NAME = "Feed Doc Cache";

    private final ICache<String, Optional<FeedDoc>> cache;
    private final FeedStore feedStore;
    private final SecurityContext securityContext;

    @Inject
    public FeedDocCache(final CacheManager cacheManager,
                        final FeedStore feedStore,
                        final SecurityContext securityContext,
                        final FeedConfig feedConfig) {
        this.feedStore = feedStore;
        this.securityContext = securityContext;
        cache = cacheManager.create(CACHE_NAME, feedConfig::getFeedDocCache, this::create);
    }

    public Optional<FeedDoc> get(final String feedName) {
        return cache.get(feedName);
    }

    private Optional<FeedDoc> create(final String feedName) {
        return securityContext.asProcessingUserResult(() -> {
            final List<DocRef> list = feedStore.findByName(feedName);
            if (list != null && list.size() > 0) {
                return Optional.ofNullable(feedStore.readDocument(list.get(0)));
            }
            return Optional.empty();
        });
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
