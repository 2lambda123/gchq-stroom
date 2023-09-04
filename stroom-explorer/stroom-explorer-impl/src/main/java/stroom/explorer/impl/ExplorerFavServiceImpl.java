package stroom.explorer.impl;

import stroom.docref.DocRef;
import stroom.docrefinfo.api.DocRefInfoService;
import stroom.explorer.api.ExplorerFavService;
import stroom.explorer.api.ExplorerService;
import stroom.security.api.SecurityContext;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Provider;

public class ExplorerFavServiceImpl implements ExplorerFavService {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ExplorerFavServiceImpl.class);

    private final ExplorerFavDao explorerFavDao;
    private final SecurityContext securityContext;
    private final Provider<DocRefInfoService> docRefInfoService;
    private final Provider<ExplorerService> explorerService;

    @Inject
    ExplorerFavServiceImpl(final ExplorerFavDao explorerFavDao,
                           final SecurityContext securityContext,
                           final Provider<DocRefInfoService> docRefInfoService,
                           final Provider<ExplorerService> explorerService) {
        this.explorerFavDao = explorerFavDao;
        this.securityContext = securityContext;
        this.docRefInfoService = docRefInfoService;
        this.explorerService = explorerService;
    }

    @Override
    public void create(final DocRef docRef) {
        final String userUuid = getCurrentUserUuid();
        explorerFavDao.createFavouriteForUser(docRef, userUuid);
        explorerService.get().rebuildTree();
    }

    @Override
    public void delete(final DocRef docRef) {
        final String userUuid = getCurrentUserUuid();
        explorerFavDao.deleteFavouriteForUser(docRef, userUuid);
        explorerService.get().rebuildTree();
    }

    @Override
    public List<DocRef> getUserFavourites() {
        final String userUuid = getCurrentUserUuid();
        return explorerFavDao.getUserFavourites(getCurrentUserUuid())
                .stream()
                .map(docRef -> {
                    try {
                        return docRefInfoService.get().decorate(docRef);
                    } catch (RuntimeException e) {
                        // Doc info couldn't be found, probably due to a document that exists in the `explorer_node`
                        // table, but not `doc`.
                        LOGGER.error("Missing doc referenced by favourite: {}, user: {}", docRef, userUuid);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean isFavourite(final DocRef docRef) {
        return explorerFavDao.isFavourite(docRef, getCurrentUserUuid());
    }

    private String getCurrentUserUuid() {
        final String userUuid = securityContext.getUserUuid();
        Objects.requireNonNull(userUuid, "No logged in user");
        return userUuid;
    }
}
