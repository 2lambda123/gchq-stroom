package stroom.proxy.app;

import stroom.proxy.app.forwarder.FailureDestinationsImpl;
import stroom.proxy.app.forwarder.ForwarderDestinationsImpl;

import java.nio.file.Path;

public class StoreAndForwardToFileTestModule extends AbstractStoreAndForwardTestModule {

    public StoreAndForwardToFileTestModule(final Config configuration,
                                           final Path configFile) {
        super(configuration, configFile);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(Sender.class).to(SenderImpl.class);
        bind(ForwarderDestinations.class).to(ForwarderDestinationsImpl.class);
        bind(FailureDestinations.class).to(FailureDestinationsImpl.class);
    }
}
