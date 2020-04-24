package stroom.importexport.impl;

import stroom.docref.DocRef;

public final class ImportExportFileNameUtil {
    private ImportExportFileNameUtil() {
        // Utility class.
    }

    public static String createFilePrefix(final DocRef docRef) {
        return  (docRef.getName() != null ? toSafeFileName(docRef.getName(), 100)  + "." : "")
                + docRef.getType() + "." + docRef.getUuid();
    }

    public static String toSafeFileName(final String string, final int maxLength) {
        String safe = string.replaceAll("[^A-Za-z0-9]", "_");
        if (safe.length() > maxLength) {
            safe = safe.substring(0, maxLength);
        }
        return safe;
    }
}
