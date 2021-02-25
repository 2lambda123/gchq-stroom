package stroom.elastic.api;

import java.util.Map;
import java.util.function.Consumer;

/**
 * A Stroom abstraction over the Elastic client library
 */
public interface ElasticIndexWriter {

    /**
     * Write a record to elastic search
     *
     * @param idFieldName      The name of the field to use as the record ID, if this is null, or there is no value for that
     *                         field, a random UUID will be generated
     * @param index            The name of the index in elastic search
     * @param type             The type of record being indexed, elastic search stores a property called _type
     * @param values           The values to write to the record
     * @param exceptionHandler Handler for any exceptions
     */
    void write(String idFieldName,
               String index,
               String type,
               Map<String, String> values,
               Consumer<Exception> exceptionHandler);
}
