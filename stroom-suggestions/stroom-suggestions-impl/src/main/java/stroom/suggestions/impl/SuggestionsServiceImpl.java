package stroom.suggestions.impl;

import stroom.query.shared.FetchSuggestionsRequest;
import stroom.query.shared.Suggestions;
import stroom.security.api.SecurityContext;
import stroom.suggestions.api.SuggestionsQueryHandler;
import stroom.suggestions.api.SuggestionsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
@SuppressWarnings("unused")
public class SuggestionsServiceImpl implements SuggestionsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuggestionsServiceImpl.class);

    private final Map<String, Provider<SuggestionsQueryHandler>> providerMap;
    private final SecurityContext securityContext;

    @Inject
    SuggestionsServiceImpl(
            final Map<String, Provider<SuggestionsQueryHandler>> providerMap,
            final SecurityContext securityContext) {
        this.providerMap = providerMap;
        this.securityContext = securityContext;
    }

    @Override
    public Suggestions fetch(final FetchSuggestionsRequest request) throws RuntimeException {
        final String dataSourceType = request.getDataSource().getType();

        return securityContext.secureResult(() -> {
            if (dataSourceType != null) {
                if (providerMap.containsKey(dataSourceType)) {
                    final SuggestionsQueryHandler queryHandler = providerMap.get(dataSourceType).get();
                    return queryHandler.getSuggestions(request);
                } else {
                    LOGGER.debug("Suggestions provider not registered for type: {}", dataSourceType);
                    return Suggestions.EMPTY;
                }
            } else {
                throw new RuntimeException("Data source type not defined");
            }
        });
    }
}
