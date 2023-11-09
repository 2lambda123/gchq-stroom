package stroom.security.identity.openid;

import stroom.cache.api.CacheManager;
import stroom.cache.api.StroomCache;
import stroom.security.identity.config.OpenIdConfig;

import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@Singleton
class AccessCodeCache {

    private static final String CACHE_NAME = "Access Code Cache";

    private final StroomCache<String, AccessCodeRequest> cache;

    @Inject
    AccessCodeCache(final CacheManager cacheManager,
                    final Provider<OpenIdConfig> openIdConfigProvider) {
        cache = cacheManager.create(
                CACHE_NAME,
                () -> openIdConfigProvider.get().getAccessCodeCache());
    }

    Optional<AccessCodeRequest> getAndRemove(final String code) {
        final Optional<AccessCodeRequest> optionalAccessCodeRequest = cache.getIfPresent(code);
        cache.remove(code);
        return optionalAccessCodeRequest;
    }

    void put(final String code, final AccessCodeRequest accessCodeRequest) {
        cache.put(code, accessCodeRequest);
    }
}
