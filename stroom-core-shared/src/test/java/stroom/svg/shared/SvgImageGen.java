package stroom.svg.shared;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A main method for walking a directory containing SVG files (some in sub dirs) and
 * doing the following:
 * <ul>
 *     <li>Replacing hex colour codes with a corresponding css colour variable.
 *     This is so we can change the icon colours at runtime.</li>
 *     <li>Output the modified svg files into stroom-app/src/main/resources/ui/images with
 *     the same names and relative paths as the source.</li>
 *     <li>Generate {@link SvgImage} enum containing all the SVGs as string values for use
 *     with inline SVGs in HTML.</li>
 * </ul>
 */
public class SvgImageGen {

    static final String CORE_SHARED_DIR = "stroom-core-shared";
    static final String APP_DIR = "stroom-app";
    static final String BASE_CLASS_NAME = "svg-image";

    private static final Pattern XML_DECLARATION_PATTERN = Pattern.compile("<\\?xml[^?]+\\?>\\n?");
    private static final Pattern XML_INDENT_PATTERN = Pattern.compile(">\\s*<");
    private static final Pattern EXCESS_WHITESPACE_PATTERN = Pattern.compile("\\s+");

    @SuppressWarnings("checkstyle:LineLength")
    private static final Pattern INKSCAPE_CLEAN_PATTERN = Pattern.compile(
            "\\s*((sodipodi:docname|inkscape:version|xmlns:(inkscape|sodipodi|svg)|id)=\"[^\"]+\"|<sodipodi:namedview[^>]*/>)");

    // #000 stays as black without a variable, e.g. hadoop.svg
    // #fff stays as white without a variable, e.g. help.svg
    // Hex colour => css colour variable
    static final Map<String, String> COLOUR_MAPPINGS = Map.ofEntries(
            Map.entry("#000000", "var(--icon-colour__black)"),
            Map.entry("#ffffff", "var(--icon-colour__white)"),
            Map.entry("#e6e1dc", "var(--icon-colour__off-white)"),
            Map.entry("#2196f4", "var(--icon-colour__xsd-background)"),
            Map.entry("#aed581", "var(--icon-colour__xsl-background)"),
            Map.entry("#ce93d8", "var(--icon-colour__xml-background)"),
            Map.entry("#2196f3", "var(--icon-colour__blue)"),
            Map.entry("#1976d2", "var(--icon-colour__info-blue)"),
            Map.entry("#d32f2f", "var(--icon-colour__red)"),
            Map.entry("#ff6f00", "var(--icon-colour__dirty-amber)"),
            Map.entry("#ff8f00", "var(--icon-colour__orange)"),
            Map.entry("#ffeb3b", "var(--icon-colour__yellow)"),
            Map.entry("#ffde81", "var(--icon-colour__pale-yellow)"),
            Map.entry("#4caf50", "var(--icon-colour__green)"),
            Map.entry("#388e3c", "var(--icon-colour__dark-green)"),
            Map.entry("#555555", "var(--icon-colour__grey)"),
            Map.entry("#9c27b0", "var(--icon-colour__purple)"),
            Map.entry("#010101", "currentColor")
    );

    private static final String ENUM_HEADER = """
            package stroom.svg.shared;

            import javax.annotation.processing.Generated;

            @Generated("@@CLASS_NAME@@")
            @SuppressWarnings({"ConcatenationWithEmptyString", "TextBlockMigration", "unused"})
            public enum SvgImage {
                // ================================================================================
                // IMPORTANT - This class is generated by @@CLASS_NAME@@
                // Do not edit it directly!
                // ================================================================================

            """.replace("@@CLASS_NAME@@", SvgImageGen.class.getName());

    private static final String ENUM_FOOTER = """


                private final String relativePathStr;
                private final String className;
                private final String svg;

                SvgImage(final String relativePathStr, final String className, final String svg) {
                    this.relativePathStr = relativePathStr;
                    this.className = className;
                    this.svg = svg;
                }

                /**
                 * @return The SVG in its XML form.
                 */
                public String getSvg() {
                    return svg;
                }

                /**
                 * @return The path of the SVG file relative to stroom-app/src/main/resources/ui/images/
                 */
                public String getRelativePathStr() {
                    return relativePathStr;
                }

                /**
                 * @return The CSS class name for the icon.
                 */
                public String getClassName() {
                    return className;
                }
            }
            """;
    private static final Path ENUM_REL_FILE_PATH = Path.of(
            "src", "main", "java", "stroom", "svg", "shared", "SvgImage.java");

    private static final Path UI_RESOURCE_REL_PATH = Path.of(
            "src", "main", "resources", "ui");

    private SvgImageGen() {
    }

    public static void main(final String[] args) throws IOException {

        final Path coreSharedPath = getCoreSharedPath();
        final Path appPath = getAppPath(coreSharedPath);
        final Path sourceBasePath = getImagesSourceBasePath(appPath);
        final Path destBasePath = getImagesDestBasePath(appPath);

        final Map<Path, SvgFile> relPathToSvgObjMap = new HashMap<>();
        final Set<String> enumFieldNameSet = new HashSet<>();

        // First transform the raw images.
        deleteDirectory(destBasePath);
        try (final Stream<Path> stream = Files.walk(sourceBasePath)) {
            stream.forEach(sourceFile -> {
                final Path relSourcePath = sourceBasePath.relativize(sourceFile);
                try {
                    String fileName = sourceFile.getFileName().toString();
                    if (fileName.toLowerCase(Locale.ROOT).endsWith(".svg")) {

                        final Path output = destBasePath.resolve(relSourcePath);
                        Files.createDirectories(output.getParent());

                        String xml = Files.readString(sourceFile, StandardCharsets.UTF_8);
                        for (final Entry<String, String> entry : COLOUR_MAPPINGS.entrySet()) {
                            final String hex = entry.getKey();
                            final String cssColourVariable = entry.getValue();
                            xml = xml.replaceAll(
                                    "(?i)" + Pattern.quote(hex),
                                    cssColourVariable);
                        }
                        // Remove the XML declaration
                        xml = XML_DECLARATION_PATTERN.matcher(xml).replaceAll("");
                        xml = XML_INDENT_PATTERN.matcher(xml).replaceAll("><");
                        xml = EXCESS_WHITESPACE_PATTERN.matcher(xml).replaceAll(" ");
                        // TODO: 06/07/2023 Would be nice to clean all the inkscape clutter from the svg
                        //  but the regex needs more work
//                        xml = INKSCAPE_CLEAN_PATTERN.matcher(xml).replaceAll("");

                        final String enumFieldName = pathToEnumFieldName(relSourcePath);
                        final String className = "svg-image__"
                                + enumFieldName.toLowerCase().replace("_", "-");
                        final boolean isNewName = enumFieldNameSet.add(enumFieldName);
                        if (!isNewName) {
                            System.err.println("Enum field name clash: " + enumFieldName);
                        }

                        final SvgFile svgFile = new SvgFile(
                                relSourcePath,
                                enumFieldName,
                                className,
                                xml);

                        if (Files.exists(output)) {
                            System.err.println("File exists: " + output);
                        } else {
//                            System.out.println(relSourcePath + " => " + output);
                            Files.writeString(output, xml);
                            relPathToSvgObjMap.put(relSourcePath, svgFile);
                        }
                    } else if (fileName.toLowerCase(Locale.ROOT).endsWith(".png")) {
                        final Path output = destBasePath.resolve(relSourcePath);
                        Files.createDirectories(output.getParent());

                        if (Files.exists(output)) {
                            System.err.println("File exists: " + output);
                        } else {
                            Files.copy(sourceFile, output);
                        }
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        // Now build the enum that contains all the XML content for using SVS inline in HTML
        final StringBuilder sb = new StringBuilder()
                .append(ENUM_HEADER);

        relPathToSvgObjMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().enumFieldName))
                .forEach(entry -> {
                    final SvgFile svgFile = entry.getValue();
                    sb.append("    ");
                    sb.append(svgFile.enumFieldName);
                    sb.append("(\"");
                    sb.append(svgFile.relPath.toString());
                    sb.append("\", \"");
                    sb.append(svgFile.className);
                    sb.append("\", \"\" +\n");

                    final String[] parts = svgFile.xml.split("\n");
                    for (String part : parts) {
                        if (part.length() == 0) {
                            sb.append("            \"\\n\" +\n");
                        } else {
                            while (part.length() > 0) {
                                int size = Math.min(80, part.length());
                                String line = part.substring(0, size);
                                part = part.substring(size);
                                line = line.replaceAll("\"", "\\\\\"");
                                line = line.replaceAll("\t", " ");
                                sb.append("            \"");
                                sb.append(line);
                                if (part.length() > 0) {
                                    sb.append("\" +\n");
                                }
                            }
                            sb.append("\\n\" +\n");
                        }
                    }
                    sb.append("            \"\"),\n\n");
                });

        sb.replace(sb.length() - 3, sb.length() - 1, ";\n");
        sb.append("    public static final String BASE_CLASS_NAME = \"");
        sb.append(BASE_CLASS_NAME);
        sb.append("\";");
        sb.append(ENUM_FOOTER);

        final Path outPath = coreSharedPath.resolve(ENUM_REL_FILE_PATH);
        try (final OutputStream outputStream = Files.newOutputStream(outPath)) {
            outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Written enum file " + outPath.toAbsolutePath());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        System.out.println("\nLight mode css variable definitions:");
        COLOUR_MAPPINGS.entrySet()
                .stream()
                .sorted(Entry.comparingByValue())
                .forEach(entry -> {
                    final String hex = entry.getKey();
                    final String variable = entry.getValue()
                            .replace("var(", "")
                            .replace(")", "");
                    if (!variable.equals("currentColor")) {
                        System.out.println("    "
                                + variable
                                + ": "
                                + hex
                                + ";");
                    }
                });

        SvgImageTools.generateUniqueColoursContactSheet();
        SvgImageTools.generateThemedIconsContactSheet();
    }

    static Path getImagesDestBasePath() {
        final Path appPath = getAppPath();
        return getImagesDestBasePath(appPath);
    }

    static Path getImagesDestBasePath(final Path appPath) {
        final Path destBasePath = appPath.resolve(UI_RESOURCE_REL_PATH)
                .resolve("images");
        return destBasePath;
    }

    static Path getImagesSourceBasePath() {
        final Path appPath = getAppPath();
        return getImagesSourceBasePath(appPath);
    }

    static Path getImagesSourceBasePath(final Path appPath) {
        final Path sourceBasePath = appPath.resolve(UI_RESOURCE_REL_PATH)
                .resolve("raw-images");
        return sourceBasePath;
    }

    static Path getAppPath(final Path coreSharedPath) {
        final Path appPath = coreSharedPath.getParent().resolve(APP_DIR);
        return appPath;
    }

    static Path getAppPath() {
        final Path coreSharedPath = getCoreSharedPath();
        return getAppPath(coreSharedPath);
    }

    static Path getCoreSharedPath() {
        Path coreSharedPath = Paths.get(".").resolve(CORE_SHARED_DIR).toAbsolutePath();
        while (!coreSharedPath.getFileName().toString().equals(CORE_SHARED_DIR)) {
            coreSharedPath = coreSharedPath.getParent();
        }
        return coreSharedPath;
    }

    static void deleteDirectory(Path directoryToBeDeleted) {
        try {
            if (Files.isDirectory(directoryToBeDeleted)) {
                Files.walk(directoryToBeDeleted)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String pathToEnumFieldName(final Path relPath) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < relPath.getNameCount(); i++) {
            if (sb.length() > 0) {
                sb.append("_");
            }
            sb.append(relPath.getName(i));
        }
        String name = sb.toString();
        name = name.substring(0, name.length() - 4)
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");

        return name;
    }


    // --------------------------------------------------------------------------------


    private record SvgFile(
            Path relPath,
            String enumFieldName,
            String className,
            String xml) {

    }
}
