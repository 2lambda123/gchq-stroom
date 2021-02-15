/*
 * Copyright 2017 Crown Copyright
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

package stroom.index.impl;

import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import org.apache.lucene.store.LockFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LockFactoryFactory {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(LockFactoryFactory.class);
    private static final LockFactory INSTANCE = new ShardLockFactory();

    private LockFactoryFactory() {
    }

    /**
     * Get a lock factory for the supplied directory.
     *
     * @param dir The directory to get the lock factory for.
     * @return A lock factory for the supplied directory.
     */
    public static LockFactory get() {
        LOGGER.trace(() -> "get()");
        return INSTANCE;

        // Old impl
        //return SimpleFSLockFactory.INSTANCE;
    }

    /**
     * Remove any lingering lock files in an index shard directory. These lock files can be left behind if the JVM
     * terminates abnormally and need to be removed when the system restarts.
     *
     * @param dir The directory to remove lock files from.
     */
    @Deprecated // Should no longer be needed if the new ShardLockFactory works ok
    public static void clean(final Path dir) {
        // Delete any lingering lock files from previous uses of the index shard.
        if (Files.isDirectory(dir)) {
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.lock")) {
                stream.forEach(file -> {
                    try {
                        Files.deleteIfExists(file);
                    } catch (final IOException e) {
                        LOGGER.error(e::getMessage, e);
                    }
                });
            } catch (final IOException e) {
                LOGGER.error(e::getMessage, e);
            }
        }
    }
}
