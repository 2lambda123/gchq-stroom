package stroom.util.config;

import stroom.util.HasHealthCheck;
import stroom.util.logging.LogUtil;

import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public abstract class AbstractFileChangeMonitor implements HasHealthCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFileChangeMonitor.class);

    private final Path monitoredFile;
    private final Path dirToWatch;
    private final ExecutorService executorService;
    private WatchService watchService = null;
    private Future<?> watcherFuture = null;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final boolean isValidFile;
    private final AtomicBoolean isFileReadScheduled = new AtomicBoolean(false);
    private final List<String> errors = new ArrayList<>();

    private static final long DELAY_BEFORE_FILE_READ_MS = 1_000;

    public AbstractFileChangeMonitor(final Path monitoredFile) {
        this.monitoredFile = Objects.requireNonNull(monitoredFile);

        if (Files.isRegularFile(monitoredFile)) {
            isValidFile = true;

            dirToWatch = monitoredFile.getParent();
            if (!Files.isDirectory(dirToWatch)) {
                throw new RuntimeException(LogUtil.message("{} is not a directory", dirToWatch));
            }
            executorService = Executors.newSingleThreadExecutor();
        } else {
            isValidFile = false;
            dirToWatch = null;
            executorService = null;
        }
    }

    protected abstract void onFileChange();

    /**
     * Starts the object. Called <i>before</i> the application becomes available.
     */
    public void start() {
        if (isValidFile) {
            try {
                startWatcher();
            } catch (Exception e) {
                // Swallow and log as we don't want to stop the app from starting just for this
                errors.add(e.getMessage());
                LOGGER.error(
                        "Unable to start file monitor for file {} due to [{}]. Changes will not be monitored.",
                        monitoredFile.toAbsolutePath(),
                        e.getMessage(),
                        e);
            }
        } else {
            LOGGER.error("Unable to start watcher as {} is not a valid file",
                    monitoredFile.toAbsolutePath().normalize());
        }
    }

    private void startWatcher() throws IOException {
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException(LogUtil.message("Error creating watch new service, {}", e.getMessage()), e);
        }

        dirToWatch.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        // run the watcher in its own thread else it will block app startup
        // TODO @AT Change to use CompleteableFuture.runAsync()
        watcherFuture = executorService.submit(() -> {
            WatchKey watchKey = null;

            LOGGER.info("Starting file modification watcher for {}",
                    monitoredFile.toAbsolutePath().normalize());
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    LOGGER.debug("Thread interrupted, stopping watching directory {}",
                            dirToWatch.toAbsolutePath().normalize());
                    break;
                }

                try {
                    isRunning.compareAndSet(false, true);
                    // block until the watch service spots a change
                    watchKey = watchService.take();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    // continue to re-use the if block above
                    continue;
                }

                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    if (LOGGER.isDebugEnabled()) {
                        if (event == null) {
                            LOGGER.debug("Event is null");
                        } else {
                            final String name = event.kind() != null
                                    ? event.kind().name()
                                    : "kind==null";
                            final String type = event.kind() != null
                                    ? event.kind().type().getSimpleName()
                                    : "kind==null";
                            LOGGER.debug("Dir watch event {}, {}, {}", name, type, event.context());
                        }
                    }

                    if (event.kind().equals(OVERFLOW)) {
                        LOGGER.warn("{} event detected breaking out. Retry file change", OVERFLOW.name());
                        break;
                    }
                    if (event.kind() != null && Path.class.isAssignableFrom(event.kind().type())) {
                        handleWatchEvent((WatchEvent<Path>) event);
                    } else {
                        LOGGER.debug("Not an event we care about");
                    }
                }
                boolean isValid = watchKey.reset();
                if (!isValid) {
                    LOGGER.warn("Watch key is no longer valid, the watch service may have been stopped");
                    break;
                }
            }
        });
    }

    private void handleWatchEvent(final WatchEvent<Path> pathEvent) {
        final WatchEvent.Kind<Path> kind = pathEvent.kind();

        // Only trigger on modify events and when count is one to avoid repeated events
        if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
            final Path modifiedFile = dirToWatch.resolve(pathEvent.context());

            try {
                // we don't care about changes to other files
                if (Files.isRegularFile(modifiedFile) && Files.isSameFile(monitoredFile, modifiedFile)) {
                    LOGGER.info("Change detected to file {}", monitoredFile.toAbsolutePath().normalize());
                    scheduleUpdateIfRequired();
                }
            } catch (IOException e) {
                // Swallow error so future changes can be monitored.
                LOGGER.error("Error comparing paths {} and {}", monitoredFile, modifiedFile, e);
            }
        }
    }

    private synchronized void scheduleUpdateIfRequired() {

        // When a file is changed the filesystem can trigger two changes, one to change the file content
        // and another to change the file access time. To prevent a duplicate read we delay the read
        // a bit so we can have many changes during that delay period but with only one read of the file.
        if (isFileReadScheduled.compareAndSet(false, true)) {
            LOGGER.info("Scheduling call to change listener for file {} in {}ms",
                    monitoredFile.toAbsolutePath().normalize(),
                    DELAY_BEFORE_FILE_READ_MS);
            CompletableFuture.delayedExecutor(DELAY_BEFORE_FILE_READ_MS, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        try {
                            synchronized (this) {
                                onFileChange();
                            }
                        } finally {
                            isFileReadScheduled.set(false);
                        }
                    });
        }
    }

    /**
     * Stops the object. Called <i>after</i> the application is no longer accepting requests.
     *
     * @throws Exception if something goes wrong.
     */
    public void stop() throws Exception {
        if (isValidFile) {
            LOGGER.info("Stopping file modification watcher for {}",
                    monitoredFile.toAbsolutePath().normalize());

            if (watchService != null) {
                watchService.close();
            }
            if (executorService != null) {
                watchService.close();
                if (watcherFuture != null
                        && !watcherFuture.isCancelled()
                        && !watcherFuture.isDone()) {
                    watcherFuture.cancel(true);
                }
                executorService.shutdown();
            }
        }
        isRunning.set(false);
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public HealthCheck.Result getHealth() {
        HealthCheck.ResultBuilder resultBuilder = HealthCheck.Result.builder();

        // isRunning will only be true if the file is also present and valid
        if (isRunning.get()) {
            resultBuilder.healthy();
        } else {
            resultBuilder
                    .unhealthy()
                    .withDetail("errors", errors);
        }

        return resultBuilder
                .withDetail("monitoredFile", monitoredFile != null
                        ? monitoredFile.toAbsolutePath().normalize().toString()
                        : null)
                .withDetail("isRunning", isRunning)
                .withDetail("isValidFile", isValidFile)
                .build();
    }
}
