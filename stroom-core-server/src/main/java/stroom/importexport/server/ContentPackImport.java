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

package stroom.importexport.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import stroom.node.server.StroomPropertyService;
import stroom.util.config.StroomProperties;
import stroom.util.io.FileUtil;
import stroom.util.spring.StroomStartup;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Component
public class ContentPackImport {

    static final String AUTO_IMPORT_ENABLED_PROP_KEY = "stroom.contentPackImportEnabled";
    static final Path CONTENT_PACK_IMPORT_DIR = Paths.get("contentPackImport");
    static final String FAILED_DIR = "failed";
    static final String IMPORTED_DIR = "imported";
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentPackImport.class);
    private ImportExportService importExportService;
    private StroomPropertyService stroomPropertyService;

    @SuppressWarnings("unused")
    @Inject
    ContentPackImport(ImportExportService importExportService, StroomPropertyService stroomPropertyService) {
        this.importExportService = importExportService;
        this.stroomPropertyService = stroomPropertyService;
    }

    //Startup with very low priority to ensure it starts after everything else
    //in particular
    @StroomStartup(priority = -1000)
    public void startup() {
        final boolean isEnabled = stroomPropertyService.getBooleanProperty(AUTO_IMPORT_ENABLED_PROP_KEY, true);

        if (isEnabled) {
            doImport();
        } else {
            LOGGER.info("Content pack import currently disabled via property: {}", AUTO_IMPORT_ENABLED_PROP_KEY);
        }
    }

    private void doImport() {
        final List<Path> contentPacksDirs = getContentPackBaseDirs();
        doImport(contentPacksDirs);
    }

    private void doImport(final List<Path> contentPacksDirs) {
        LOGGER.info("ContentPackImport started");

        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger failedCounter = new AtomicInteger();

        contentPacksDirs.forEach(contentPacksDir -> {
            if (!Files.isDirectory(contentPacksDir)) {
                LOGGER.warn("Content packs directory {} doesn't exist", FileUtil.getCanonicalPath(contentPacksDir));

            } else {
                LOGGER.info("Processing content packs in directory {}", FileUtil.getCanonicalPath(contentPacksDir));

                try (final DirectoryStream<Path> stream = Files.newDirectoryStream(contentPacksDir, "*.zip")) {
                    stream.forEach(file -> {
                        try {
                            boolean result = importContentPack(contentPacksDir, file);
                            if (result) {
                                successCounter.incrementAndGet();
                            } else {
                                failedCounter.incrementAndGet();
                            }
                        } catch (final Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    });
                } catch (final IOException e) {
                    LOGGER.error("Unable to read content pack files from {}", FileUtil.getCanonicalPath(contentPacksDir), e);
                }
            }
        });
        LOGGER.info("Content pack import counts - success: {}, failed: {}",
                successCounter.get(),
                failedCounter.get());

        LOGGER.info("ContentPackImport finished");
    }

    private boolean importContentPack(Path parentPath, Path contentPack) {
        LOGGER.info("Starting import of content pack {}", FileUtil.getCanonicalPath(contentPack));

        try {
            //It is possible to import a content pack (or packs) with missing dependencies
            //so the onus is on the person putting the file in the import directory to
            //ensure the packs they import are complete
            importExportService.performImportWithoutConfirmation(contentPack);
            moveFile(contentPack, contentPack.getParent().resolve(IMPORTED_DIR));

            LOGGER.info("Completed import of content pack {}", FileUtil.getCanonicalPath(contentPack));

        } catch (Exception e) {
            LOGGER.error("Error importing content pack {}", FileUtil.getCanonicalPath(contentPack), e);
            moveFile(contentPack, contentPack.getParent().resolve(FAILED_DIR));
            return false;
        }
        return true;
    }

    private void moveFile(Path contentPack, Path destDir) {
        Path destPath = destDir.resolve(contentPack.getFileName());
        try {
            //make sure the directory exists
            Files.createDirectories(destDir);
            Files.move(contentPack, destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error moving file from %s to %s",
                    FileUtil.getCanonicalPath(contentPack), FileUtil.getCanonicalPath(destPath)));
        }
    }

    private List<Path> getContentPackBaseDirs() {
        return Arrays.asList(getApplicationJarDir(), getConfigDir()).stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(path -> path.resolve(CONTENT_PACK_IMPORT_DIR))
                .collect(Collectors.toList());
    }

    private Optional<Path> getConfigDir() {
        return Optional.of(StroomProperties.getConfigDir());
    }


    private Optional<Path> getApplicationJarDir() {
        try {
            //This isn't ideal when running in junit, as it will be the location of the junit class
            //however it won't find any zips in here so will carry on regardless
            String codeSourceLocation = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            return Optional.of(Paths.get(codeSourceLocation).getParent());
        } catch (Exception e) {
            LOGGER.warn("Unable to determine application jar directory due to: {}", e.getMessage());
            return Optional.empty();
        }
    }

}
