package stroom.receive;

import stroom.cache.impl.CacheModule;
import stroom.collection.mock.MockCollectionModule;
import stroom.core.receive.ReceiveDataModule;
import stroom.data.store.mock.MockStreamStoreModule;
import stroom.dictionary.impl.DictionaryModule;
import stroom.docstore.impl.DocStoreModule;
import stroom.docstore.impl.memory.MemoryPersistenceModule;
import stroom.event.logging.api.DocumentEventLog;
import stroom.feed.impl.FeedModule;
import stroom.legacy.impex_6_1.LegacyImpexModule;
import stroom.meta.mock.MockMetaModule;
import stroom.meta.statistics.impl.MockMetaStatisticsModule;
import stroom.receive.rules.impl.ReceiveDataRuleSetModule;
import stroom.security.api.RequestAuthenticator;
import stroom.security.mock.MockSecurityContextModule;
import stroom.task.impl.TaskContextModule;
import stroom.util.entityevent.EntityEventBus;
import stroom.util.pipeline.scope.PipelineScopeModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.util.Providers;

import java.util.Optional;

public class TestBaseModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new CacheModule());
        install(new DictionaryModule());
        install(new DocStoreModule());
        install(new FeedModule());
        install(new LegacyImpexModule());
        install(new MemoryPersistenceModule());
        install(new MockMetaModule());
        install(new MockMetaStatisticsModule());
        install(new MockSecurityContextModule());
        install(new MockStreamStoreModule());
        install(new PipelineScopeModule());
        install(new ReceiveDataModule());
        install(new ReceiveDataRuleSetModule());
        install(new MockCollectionModule());
        install(new TaskContextModule());

        bind(DocumentEventLog.class).toProvider(Providers.of(null));
    }

    @Provides
    EntityEventBus entityEventBus() {
        return event -> {
        };
    }

    @Provides
    RequestAuthenticator requestAuthenticator() {
        return request -> Optional.empty();
    }
}
