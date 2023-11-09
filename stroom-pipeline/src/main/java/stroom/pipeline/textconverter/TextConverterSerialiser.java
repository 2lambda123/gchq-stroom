package stroom.pipeline.textconverter;

import stroom.docstore.api.DocumentSerialiser2;
import stroom.docstore.api.Serialiser2;
import stroom.docstore.api.Serialiser2Factory;
import stroom.pipeline.shared.TextConverterDoc;
import stroom.util.string.EncodingUtil;

import java.io.IOException;
import java.util.Map;
import jakarta.inject.Inject;

public class TextConverterSerialiser implements DocumentSerialiser2<TextConverterDoc> {

    private static final String XML = "xml";

    private final Serialiser2<TextConverterDoc> delegate;

    @Inject
    public TextConverterSerialiser(final Serialiser2Factory serialiser2Factory) {
        this.delegate = serialiser2Factory.createSerialiser(TextConverterDoc.class);
    }

    @Override
    public TextConverterDoc read(final Map<String, byte[]> data) throws IOException {
        final TextConverterDoc document = delegate.read(data);
        document.setData(EncodingUtil.asString(data.get(XML)));
        return document;
    }

    @Override
    public Map<String, byte[]> write(final TextConverterDoc document) throws IOException {
        final String xml = document.getData();
        document.setData(null);

        final Map<String, byte[]> map = delegate.write(document);
        if (xml != null) {
            map.put(XML, EncodingUtil.asBytes(xml));
            document.setData(xml);
        }

        return map;
    }
}
