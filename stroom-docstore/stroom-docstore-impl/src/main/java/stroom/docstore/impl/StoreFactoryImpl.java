package stroom.docstore.impl;

import stroom.docrefinfo.api.DocRefInfoService;
import stroom.docstore.api.DocumentSerialiser2;
import stroom.docstore.api.Store;
import stroom.docstore.api.StoreFactory;
import stroom.docstore.shared.Doc;
import stroom.importexport.api.ImportConverter;
import stroom.security.api.SecurityContext;
import stroom.util.entityevent.EntityEventBus;

import javax.inject.Inject;
import javax.inject.Provider;

public class StoreFactoryImpl implements StoreFactory {

    private final Persistence persistence;
    private final EntityEventBus entityEventBus;
    private final ImportConverter importConverter;
    private final SecurityContext securityContext;
    private final Provider<DocRefInfoService> docRefInfoServiceProvider;

    @Inject
    public StoreFactoryImpl(final Persistence persistence,
                            final EntityEventBus entityEventBus,
                            final ImportConverter importConverter,
                            final SecurityContext securityContext,
                            final Provider<DocRefInfoService> docRefInfoServiceProvider) {
        this.persistence = persistence;
        this.entityEventBus = entityEventBus;
        this.importConverter = importConverter;
        this.securityContext = securityContext;
        this.docRefInfoServiceProvider = docRefInfoServiceProvider;
    }

    @Override
    public <D extends Doc> Store<D> createStore(final DocumentSerialiser2<D> serialiser,
                                                final String type,
                                                final Class<D> clazz) {
        return new StoreImpl<>(
                persistence,
                entityEventBus,
                importConverter,
                securityContext,
                docRefInfoServiceProvider,
                serialiser,
                type,
                clazz);
    }
}
