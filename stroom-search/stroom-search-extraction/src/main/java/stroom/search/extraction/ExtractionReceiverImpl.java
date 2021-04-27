package stroom.search.extraction;

import stroom.dashboard.expression.v1.FieldIndex;
import stroom.dashboard.expression.v1.Val;
import stroom.query.common.v2.ReceiverImpl;

import java.util.function.Consumer;

public class ExtractionReceiverImpl extends ReceiverImpl implements ExtractionReceiver {

    private final FieldIndex fieldIndex;

    public ExtractionReceiverImpl(final Consumer<Val[]> valuesConsumer,
                              final Consumer<Throwable> errorConsumer,
                              final Consumer<Long> completionConsumer,
                              final FieldIndex fieldIndex) {
        super(valuesConsumer, errorConsumer, completionConsumer);
        this.fieldIndex = fieldIndex;
    }

    public FieldIndex getFieldMap() {
        return fieldIndex;
    }
}
