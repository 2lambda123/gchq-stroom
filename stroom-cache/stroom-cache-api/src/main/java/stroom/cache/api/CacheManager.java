/*
 * Copyright 2019 Crown Copyright
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

package stroom.cache.api;

import stroom.util.cache.CacheConfig;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface CacheManager extends AutoCloseable {

    <K, V> StroomCache<K, V> create(
            final String name,
            final Supplier<CacheConfig> cacheConfigSupplier,
            final BiConsumer<K, V> removalNotificationConsumer);


    <K, V> LoadingStroomCache<K, V> createLoadingCache(
            final String name,
            final Supplier<CacheConfig> cacheConfigSupplier,
            final Function<K, V> loadFunction,
            final BiConsumer<K, V> removalNotificationConsumer);

    void close();

    default <K, V> StroomCache<K, V> create(
            final String name,
            final Supplier<CacheConfig> cacheConfigSupplier) {

        return create(
                name,
                cacheConfigSupplier,
                null);
    }

    default <K, V> LoadingStroomCache<K, V> createLoadingCache(
            final String name,
            final Supplier<CacheConfig> cacheConfigSupplier, Function<K, V> loadFunction) {

        return createLoadingCache(
                name,
                cacheConfigSupplier,
                loadFunction,
                null);
    }
}
