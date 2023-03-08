/*
 * Copyright 2016 Crown Copyright
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

package stroom.resource.impl;

import stroom.resource.api.ResourceStore;
import stroom.task.api.TaskContextFactory;
import stroom.util.io.FileUtil;
import stroom.util.io.TempDirProvider;
import stroom.util.shared.ResourceKey;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Simple Store that gives you 1 hour to use your temp file and then it deletes
 * it.
 */
@Singleton
public class ResourceStoreImpl implements ResourceStore {

    private final TempDirProvider tempDirProvider;
    private final TaskContextFactory taskContextFactory;

    private Set<ResourceKey> currentFiles = new HashSet<>();
    private Set<ResourceKey> oldFiles = new HashSet<>();
    private long sequence;

    @Inject
    public ResourceStoreImpl(final TempDirProvider tempDirProvider,
                             final TaskContextFactory taskContextFactory) {
        this.tempDirProvider = tempDirProvider;
        this.taskContextFactory = taskContextFactory;
    }

    private Path getTempDir() {
        final Path tempDir = tempDirProvider.get().resolve("resources");
        try {
            Files.createDirectories(tempDir);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return tempDir;
    }

    void startup() {
        FileUtil.deleteContents(getTempDir());
    }

    void shutdown() {
        FileUtil.deleteContents(getTempDir());
    }

    @Override
    public synchronized ResourceKey createTempFile(final String name) {
        final String fileName = FileUtil.getCanonicalPath(getTempDir().resolve((sequence++) + name));
        final ResourceKey resourceKey = new ResourceKey(name, fileName);
        currentFiles.add(resourceKey);

        return resourceKey;
    }

    @Override
    public synchronized void deleteTempFile(final ResourceKey resourceKey) {
        currentFiles.remove(resourceKey);
        oldFiles.remove(resourceKey);
        final Path file = Paths.get(resourceKey.getKey());
        try {
            Files.deleteIfExists(file);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public synchronized Path getTempFile(final ResourceKey resourceKey) {
        // File gone !
        if (!currentFiles.contains(resourceKey) && !oldFiles.contains(resourceKey)) {
            return null;
        }
        return Paths.get(resourceKey.getKey());
    }

    void execute() {
        taskContextFactory.current().info(() -> "Deleting temp files");
        flipStore();
    }

    /**
     * Move the current files to the old files deleting the old ones.
     */
    private synchronized void flipStore() {
        final Set<ResourceKey> clonedOldFiles = new HashSet<>(oldFiles);
        clonedOldFiles.forEach(this::deleteTempFile);
        oldFiles = currentFiles;
        currentFiles = new HashSet<>();
    }
}
