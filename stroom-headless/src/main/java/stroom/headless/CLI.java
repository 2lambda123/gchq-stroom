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

package stroom.headless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import stroom.dictionary.spring.DictionaryConfiguration;
import stroom.entity.server.util.XMLUtil;
import stroom.explorer.server.ExplorerConfiguration;
import stroom.importexport.server.ImportExportService;
import stroom.node.server.NodeCache;
import stroom.node.server.VolumeService;
import stroom.node.shared.Volume;
import stroom.node.shared.Volume.VolumeType;
import stroom.pipeline.spring.PipelineConfiguration;
import stroom.proxy.repo.StroomZipFile;
import stroom.proxy.repo.StroomZipFileType;
import stroom.proxy.repo.StroomZipNameSet;
import stroom.proxy.repo.StroomZipRepository;
import stroom.spring.PersistenceConfiguration;
import stroom.spring.ScopeConfiguration;
import stroom.spring.ServerConfiguration;
import stroom.task.server.GenericServerTask;
import stroom.task.server.TaskManager;
import stroom.util.AbstractCommandLineTool;
import stroom.util.config.StroomProperties;
import stroom.util.config.StroomProperties.Source;
import stroom.util.io.FileUtil;
import stroom.util.io.IgnoreCloseInputStream;
import stroom.util.io.StreamUtil;
import stroom.util.shared.ModelStringUtil;
import stroom.util.spring.StroomSpringProfiles;
import stroom.util.task.ExternalShutdownController;
import stroom.util.task.TaskScopeRunnable;

import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Command line tool to process some files from a proxy stroom.
 */
public class CLI extends AbstractCommandLineTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(CLI.class);

    private ApplicationContext appContext = null;

    private String input;
    private String error;
    private String config;
    private String tmp;

    private Path inputDir;
    private Path errorFile;
    private Path configFile;
    private Path tmpDir;

    public static void main(final String[] args) throws Exception {
        new CLI().doMain(args);
    }

    public void setInput(final String input) {
        this.input = input;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public void setConfig(final String config) {
        this.config = config;
    }

    public void setTmp(final String tmp) {
        this.tmp = tmp;

        final Path tempDir = Paths.get(tmp);

        // Redirect the temp dir for headless.
        StroomProperties.setOverrideProperty(StroomProperties.STROOM_TEMP, FileUtil.getCanonicalPath(tempDir), Source.USER_CONF);

        FileUtil.forgetTempDir();
    }

    @Override
    protected void checkArgs() {
        if (input == null) {
            failArg("input", "required");
        }
        if (error == null) {
            failArg("error", "required");
        }
        if (config == null) {
            failArg("config", "required");
        }
        if (tmp == null) {
            failArg("tmp", "required");
        }
    }

    private void init() {
        inputDir = Paths.get(input);
        errorFile = Paths.get(error);
        configFile = Paths.get(config);
        tmpDir = Paths.get(tmp);

        if (!Files.isDirectory(inputDir)) {
            throw new RuntimeException("Input directory \"" + FileUtil.getCanonicalPath(inputDir) + "\" cannot be found!");
        }
        if (!Files.isDirectory(errorFile.getParent())) {
            throw new RuntimeException("Output file \"" + FileUtil.getCanonicalPath(errorFile.getParent())
                    + "\" parent directory cannot be found!");
        }
        if (!Files.isRegularFile(configFile)) {
            throw new RuntimeException("Config file \"" + FileUtil.getCanonicalPath(configFile) + "\" cannot be found!");
        }

        // Make sure tmp dir exists and is empty.
        FileUtil.mkdirs(tmpDir);
        FileUtil.deleteFile(errorFile);
        FileUtil.deleteContents(tmpDir);
    }

    @Override
    public void run() {
        try {
            StroomProperties.setOverrideProperty("stroom.jpaHbm2DdlAuto", "update", Source.TEST);

            StroomProperties.setOverrideProperty("stroom.jdbcDriverClassName", "org.hsqldb.jdbcDriver", Source.TEST);
            StroomProperties.setOverrideProperty("stroom.jpaDialect", "org.hibernate.dialect.HSQLDialect", Source.TEST);
            StroomProperties.setOverrideProperty("stroom.jdbcDriverUrl", "jdbc:hsqldb:file:${stroom.temp}/stroom/HSQLDB.DAT;shutdown=true", Source.TEST);
            StroomProperties.setOverrideProperty("stroom.jdbcDriverUsername", "sa", Source.TEST);
            StroomProperties.setOverrideProperty("stroom.jdbcDriverPassword", "", Source.TEST);

            StroomProperties.setOverrideProperty("stroom.statistics.sql.jdbcDriverClassName", "org.hsqldb.jdbcDriver", Source.TEST);
            StroomProperties.setOverrideProperty("stroom.statistics.sql.jpaDialect", "org.hibernate.dialect.HSQLDialect", Source.TEST);
            StroomProperties.setOverrideProperty("stroom.statistics.sql.jdbcDriverUrl", "jdbc:hsqldb:file:${stroom.temp}/statistics/HSQLDB.DAT;shutdown=true", Source.TEST);
            StroomProperties.setOverrideProperty("stroom.statistics.sql.jdbcDriverUsername", "sa", Source.TEST);
            StroomProperties.setOverrideProperty("stroom.statistics.sql.jdbcDriverPassword", "", Source.TEST);

            StroomProperties.setOverrideProperty("stroom.lifecycle.enabled", "false", Source.TEST);

            new TaskScopeRunnable(GenericServerTask.create("Headless Stroom", null)) {
                @Override
                protected void exec() {
                    process();
                }
            }.run();
        } finally {
            StroomProperties.removeOverrides();

            ExternalShutdownController.shutdown();
        }
    }

    private void process() {
        final long startTime = System.currentTimeMillis();

        // Initialise some variables.
        init();

        // Read the configuration.
        readConfig();

        Writer errorWriter = null;
        try {
            // Create the required output stream writer.
            final OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(errorFile));
            errorWriter = new OutputStreamWriter(outputStream, StreamUtil.DEFAULT_CHARSET);

            // Create an XML writer.
            final TransformerHandler th = XMLUtil.createTransformerHandler(true);
            th.setResult(new StreamResult(errorWriter));


            processRepository(errorWriter);


        } catch (final Throwable e) {
            LOGGER.error("Unable to process headless", e);
        } finally {
            try {
                // Close the output stream writer.
                if (errorWriter != null) {
                    errorWriter.flush();
                    errorWriter.close();
                }
            } catch (final IOException e) {
                LOGGER.error("Unable to flush and close outputStreamWriter", e);
            }
        }

        LOGGER.info("Processing completed in "
                + ModelStringUtil.formatDurationString(System.currentTimeMillis() - startTime));
    }

    private void processRepository(final Writer errorWriter) {
        try {
            final TaskManager taskManager = getAppContext().getBean(TaskManager.class);

            // Loop over all of the data files in the repository.
            final StroomZipRepository repo = new StroomZipRepository(FileUtil.getCanonicalPath(inputDir), true);
            final List<Path> zipFiles = repo.listAllZipFiles();
            zipFiles.sort(Comparator.naturalOrder());
            try (final Stream<Path> stream = zipFiles.stream()) {
                stream.forEach(p -> {
                    try {
                        LOGGER.info("Processing: " + FileUtil.getCanonicalPath(p));

                        final StroomZipFile stroomZipFile = new StroomZipFile(p);
                        final StroomZipNameSet nameSet = stroomZipFile.getStroomZipNameSet();

                        // Process each base file in a consistent order
                        for (final String baseName : nameSet.getBaseNameList()) {
                            final InputStream dataStream = stroomZipFile.getInputStream(baseName, StroomZipFileType.Data);
                            final InputStream metaStream = stroomZipFile.getInputStream(baseName, StroomZipFileType.Meta);
                            final InputStream contextStream = stroomZipFile.getInputStream(baseName, StroomZipFileType.Context);

                            final CLITranslationTask task = new CLITranslationTask(
                                    IgnoreCloseInputStream.wrap(dataStream), IgnoreCloseInputStream.wrap(metaStream),
                                    IgnoreCloseInputStream.wrap(contextStream), errorWriter);
                            taskManager.exec(task);
                        }

                        // Close the zip file.
                        stroomZipFile.close();
                    } catch (final Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });
            }
        } catch (final Exception e) {
            LOGGER.error("Unable to process repository!", e);
        }
    }

    private void readConfig() {
        LOGGER.info("Reading configuration from: " + FileUtil.getCanonicalPath(configFile));

        final ImportExportService importExportService = getAppContext().getBean(ImportExportService.class);
        importExportService.performImportWithoutConfirmation(configFile);

        final NodeCache nodeCache = getAppContext().getBean(NodeCache.class);
        final VolumeService volumeService = getAppContext().getBean(VolumeService.class);
        volumeService
                .save(Volume.create(nodeCache.getDefaultNode(), FileUtil.getCanonicalPath(tmpDir) + "/cvol", VolumeType.PUBLIC));
    }

    private ApplicationContext getAppContext() {
        if (appContext == null) {
            appContext = buildAppContext();
        }
        return appContext;
    }

    private ApplicationContext buildAppContext() {
        System.setProperty("spring.profiles.active", StroomSpringProfiles.PROD + ", Headless");
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
//                DashboardConfiguration.class,
//                EventLoggingConfiguration.class,
//                IndexConfiguration.class,
//                MetaDataStatisticConfiguration.class,
                PersistenceConfiguration.class,
                DictionaryConfiguration.class,
                PipelineConfiguration.class,
                ScopeConfiguration.class,
//                ScriptConfiguration.class,
//                SearchConfiguration.class,
//                SecurityConfiguration.class,
                ExplorerConfiguration.class,
                ServerConfiguration.class,
//                StatisticsConfiguration.class,
//                VisualisationConfiguration.class,
                HeadlessConfiguration.class);
        return context;
    }
}
