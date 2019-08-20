package stroom.docstore.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JsonSerialiser<D> implements Serialiser<D> {
    private final ObjectMapper mapper;

    public JsonSerialiser() {
        this.mapper = getMapper(true);
    }

    @Override
    public D read(final InputStream inputStream, final Class<D> clazz) throws IOException {
        return mapper.readValue(inputStream, clazz);
    }

    @Override
    public void write(final OutputStream outputStream, final D document) throws IOException {
        write(outputStream, document, false);
    }

    @Override
    public void write(final OutputStream outputStream, final D document, final boolean export) throws IOException {
        mapper.writeValue(outputStream, document);
    }

    private ObjectMapper getMapper(final boolean indent) {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, indent);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Enabling default typing adds type information where it would otherwise be ambiguous, i.e. for abstract classes
//        mapper.enableDefaultTyping();
        return mapper;
    }
}
