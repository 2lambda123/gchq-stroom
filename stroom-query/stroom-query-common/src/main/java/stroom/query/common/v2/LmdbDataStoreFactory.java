package stroom.query.common.v2;

import stroom.dashboard.expression.v1.FieldIndex;
import stroom.lmdb.LmdbConfig;
import stroom.lmdb.LmdbEnvFactory;
import stroom.query.api.v2.TableSettings;
import stroom.util.NullSafe;
import stroom.util.io.FileUtil;
import stroom.util.io.PathCreator;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton // To ensure the localDir delete is done only once and before store creation
public class LmdbDataStoreFactory implements DataStoreFactory {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(LmdbDataStoreFactory.class);

    private final LmdbEnvFactory lmdbEnvFactory;
    private final ResultStoreConfig resultStoreConfig;
    private final Provider<Executor> executorProvider;

    @Inject
    public LmdbDataStoreFactory(final LmdbEnvFactory lmdbEnvFactory,
                                final ResultStoreConfig resultStoreConfig,
                                final PathCreator pathCreator,
                                final Provider<Executor> executorProvider) {
        this.lmdbEnvFactory = lmdbEnvFactory;
        this.resultStoreConfig = resultStoreConfig;
        this.executorProvider = executorProvider;
        // As result stores are transient they serve no purpose after shutdown so delete any that
        // may still be there
        cleanStoresDir(pathCreator);
    }

    @Override
    public DataStore create(final String queryKey,
                            final String componentId,
                            final TableSettings tableSettings,
                            final FieldIndex fieldIndex,
                            final Map<String, String> paramMap,
                            final Sizes maxResults,
                            final Sizes storeSize,
                            final boolean producePayloads,
                            final ErrorConsumer errorConsumer) {

        if (!resultStoreConfig.isOffHeapResults()) {
            if (producePayloads) {
                throw new RuntimeException("MapDataStore cannot produce payloads");
            }

            return new MapDataStore(
                    tableSettings,
                    fieldIndex,
                    paramMap,
                    maxResults,
                    storeSize,
                    errorConsumer);
        } else {
            return new LmdbDataStore(
                    lmdbEnvFactory,
                    resultStoreConfig,
                    queryKey,
                    componentId,
                    tableSettings,
                    fieldIndex,
                    paramMap,
                    maxResults,
                    producePayloads,
                    executorProvider,
                    errorConsumer);
        }
    }

    private void cleanStoresDir(final PathCreator pathCreator) {
        final String dirFromConfig = NullSafe.get(
                resultStoreConfig,
                ResultStoreConfig::getLmdbConfig,
                LmdbConfig::getLocalDir);

        Objects.requireNonNull(dirFromConfig, "localDir not set");

        final String localDirStr = pathCreator.makeAbsolute(
                pathCreator.replaceSystemProperties(dirFromConfig));
        final Path localDir = Paths.get(localDirStr);

        LOGGER.info("Deleting redundant search result stores from {}", localDir);
        // Delete contents.
        if (!FileUtil.deleteContents(localDir)) {
            throw new RuntimeException(LogUtil.message("Error deleting contents of {}", localDir));
        }
    }
}
