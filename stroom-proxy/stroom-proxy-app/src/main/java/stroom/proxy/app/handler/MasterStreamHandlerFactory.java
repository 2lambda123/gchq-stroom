package stroom.proxy.app.handler;

import stroom.proxy.repo.ProxyRepositoryStreamHandlerFactory;
import stroom.proxy.repo.StreamHandler;
import stroom.proxy.repo.StreamHandlerFactory;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MasterStreamHandlerFactory implements StreamHandlerFactory {

    private final ProxyRepositoryStreamHandlerFactory proxyRepositoryStreamHandlerFactory;
    private final ForwardStreamHandlerFactory forwardStreamHandlerFactory;

    @Inject
    MasterStreamHandlerFactory(final ProxyRepositoryStreamHandlerFactory proxyRepositoryStreamHandlerFactory,
                               final ForwardStreamHandlerFactory forwardStreamHandlerFactory) {
        this.proxyRepositoryStreamHandlerFactory = proxyRepositoryStreamHandlerFactory;
        this.forwardStreamHandlerFactory = forwardStreamHandlerFactory;
    }

    @Override
    public List<StreamHandler> addReceiveHandlers(final List<StreamHandler> handlers) {
        proxyRepositoryStreamHandlerFactory.addReceiveHandlers(handlers);
        forwardStreamHandlerFactory.addReceiveHandlers(handlers);
        return handlers;
    }

    @Override
    public List<StreamHandler> addSendHandlers(final List<StreamHandler> handlers) {
        proxyRepositoryStreamHandlerFactory.addSendHandlers(handlers);
        forwardStreamHandlerFactory.addSendHandlers(handlers);
        return handlers;
    }
}
