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

package stroom.kafka.impl;

import stroom.cache.api.CacheManager;
import stroom.cache.api.ICache;
import stroom.docref.DocRef;
import stroom.kafka.shared.KafkaConfigDoc;
import stroom.security.api.SecurityContext;
import stroom.util.shared.Clearable;

import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KafkaConfigDocCache implements Clearable {

    private static final String CACHE_NAME = "Kafka Config Doc Cache";

    private final ICache<DocRef, Optional<KafkaConfigDoc>> cache;
    private final KafkaConfigStore kafkaConfigStore;
    private final SecurityContext securityContext;

    @Inject
    public KafkaConfigDocCache(final CacheManager cacheManager,
                               final KafkaConfigStore kafkaConfigStore,
                               final KafkaConfig kafkaConfig,
                               final SecurityContext securityContext) {
        this.kafkaConfigStore = kafkaConfigStore;
        this.securityContext = securityContext;
        cache = cacheManager.create(CACHE_NAME, kafkaConfig::getKafkaConfigDocCache, this::create);
    }

    public Optional<KafkaConfigDoc> get(final DocRef kafkaConfigDocRef) {
        return cache.get(kafkaConfigDocRef);
    }

    private Optional<KafkaConfigDoc> create(final DocRef kafkaConfigDocRef) {
        Objects.requireNonNull(kafkaConfigDocRef);
        return securityContext.asProcessingUserResult(() ->
                Optional.ofNullable(kafkaConfigStore.readDocument(kafkaConfigDocRef)));
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
