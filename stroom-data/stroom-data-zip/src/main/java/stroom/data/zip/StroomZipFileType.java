package stroom.data.zip;

import stroom.util.NullSafe;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum StroomZipFileType {
    MANIFEST(0, "mf", "manifest"),
    META(1, "meta", "hdr", "header", "met"),
    CONTEXT(2, "ctx", "context"),
    DATA(3, "dat");

    private static final Map<String, StroomZipFileType> EXTENSION_MAP = Arrays.stream(StroomZipFileType.values())
            .flatMap(stroomZipFileType ->
                    Stream.concat(
                                    Stream.of(stroomZipFileType.extension),
                                    stroomZipFileType.extensionAliases.stream())
                            .map(ext -> Map.entry(ext, stroomZipFileType)))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    private final int index;
    private final String extension;
    private final Set<String> extensionAliases;

    StroomZipFileType(final int index,
                      final String extension,
                      final String... extensionAliases) {
        this.index = index;
        this.extension = extension;
        this.extensionAliases = NullSafe.asSet(extensionAliases);
    }

    public int getIndex() {
        return index;
    }

    /**
     * The official extension for the file type, e.g. 'dat'.
     *
     * @return The official extension for the file type.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Other known and possibly legacy extension(s) for this file type, e.g. 'hdr'.
     */
    public Set<String> getExtensionAliases() {
        return extensionAliases;
    }

    /**
     * Convenience method to return the extension with a `.` in front of it, e.g. '.dat'.
     */
    public String getDotExtension() {
        return "." + extension;
    }

    /**
     * @return True if fileName ends with this extension (or one of the aliases).
     */
    public boolean hasExtension(final String fileName) {
        return fileName != null
                && (fileName.endsWith(getDotExtension())
                || getExtensionAliases().stream().anyMatch(ext -> fileName.endsWith("." + ext)));
    }

    /**
     * @return True if fileName ends with the official extension for this type, but NOT an alias extension
     */
    public boolean hasOfficialExtension(final String fileName) {
        return fileName != null
                && fileName.endsWith(getDotExtension());
    }

    public static StroomZipFileType fromExtension(final String extension) {
        Optional<StroomZipFileType> optional = Optional.empty();
        if (extension != null && !extension.isEmpty()) {
            optional = Optional.ofNullable(EXTENSION_MAP.get(extension.toLowerCase(Locale.ROOT)));
        }
        return optional.orElse(StroomZipFileType.DATA);
    }

    /**
     * @return True if the passed extension is one known to stroom
     */
    public static boolean isKnownExtension(final String extension) {
        if (NullSafe.isEmptyString(extension)) {
            return false;
        }else {
            return EXTENSION_MAP.containsKey(extension.toLowerCase(Locale.ROOT));
        }
    }
}
