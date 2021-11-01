package stroom.data.shared;

import java.util.Set;

public class StreamTypeNames {

    /**
     * Saved raw version for the archive.
     */
    public static final String RAW_EVENTS = "Raw Events";
    /**
     * Saved raw version for the archive.
     */
    public static final String RAW_REFERENCE = "Raw Reference";
    /**
     * Processed events Data files.
     */
    public static final String EVENTS = "Events";
    /**
     * Processed reference Data files.
     */
    public static final String REFERENCE = "Reference";
    /**
     * Processed Data files conforming to the Records XMLSchema.
     */
    public static final String RECORDS = "Records";
    /**
     * Meta meta data
     */
    public static final String META = "Meta Data";
    /**
     * Processed events Data files.
     */
    public static final String ERROR = "Error";
    /**
     * Context file for use with an events file.
     */
    public static final String CONTEXT = "Context";

    /**
     * Processed test events data files
     */
    public static final String TEST_EVENTS = "Test Events";

    /**
     * Processed test reference data files
     */
    public static final String TEST_REFERENCE = "Test Reference";

    /**
     * Processed detections
     */
    public static final String DETECTIONS = "Detections";

    public static final Set<String> VALID_RECEIVE_TYPE_NAMES = Set.of(
            RAW_EVENTS,
            RAW_REFERENCE,
            EVENTS,
            REFERENCE,
            TEST_EVENTS,
            TEST_REFERENCE);
}
