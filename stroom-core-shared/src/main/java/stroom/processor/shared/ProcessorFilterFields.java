package stroom.processor.shared;

import stroom.datasource.api.v2.BooleanField;
import stroom.datasource.api.v2.IdField;
import stroom.datasource.api.v2.IntegerField;
import stroom.datasource.api.v2.LongField;
import stroom.datasource.api.v2.QueryField;
import stroom.datasource.api.v2.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProcessorFilterFields {

    public static final String FIELD_ID = "Id";

    private static final List<QueryField> FIELDS = new ArrayList<>();
    private static final Map<String, QueryField> FIELD_MAP;

    public static final IdField ID = new IdField("Processor Filter Id");
//    public static final TextField CREATE_USER = new TextField("Processor Filter Create User");
    public static final TextField OWNER_UUID = new TextField("Processor Filter Owner User UUID");
    public static final LongField LAST_POLL_MS = new LongField("Processor Filter Last Poll Ms");
    public static final IntegerField PRIORITY = new IntegerField("Processor Filter Priority");
    public static final BooleanField ENABLED = new BooleanField("Processor Filter Enabled");
    public static final BooleanField DELETED = new BooleanField("Processor Filter Deleted");
    public static final IdField PROCESSOR_ID = new IdField("Processor Id");
    public static final TextField UUID = new TextField("Processor Filter UUID");

    static {
        FIELDS.add(ID);
        FIELDS.add(OWNER_UUID);
        FIELDS.add(LAST_POLL_MS);
        FIELDS.add(PRIORITY);
        FIELDS.add(ENABLED);
        FIELDS.add(DELETED);
        FIELDS.add(PROCESSOR_ID);
        FIELDS.add(UUID);
        FIELD_MAP = FIELDS.stream().collect(Collectors.toMap(QueryField::getName, Function.identity()));
    }

    public static List<QueryField> getFields() {
        return new ArrayList<>(FIELDS);
    }

    public static Map<String, QueryField> getFieldMap() {
        return FIELD_MAP;
    }
}
