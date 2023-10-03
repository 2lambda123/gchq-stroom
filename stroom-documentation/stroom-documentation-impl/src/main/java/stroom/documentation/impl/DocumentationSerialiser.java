package stroom.documentation.impl;

import stroom.docstore.api.DocumentSerialiser2;
import stroom.docstore.api.Serialiser2;
import stroom.docstore.api.Serialiser2Factory;
import stroom.documentation.shared.DocumentationDoc;
import stroom.util.string.EncodingUtil;

import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;

public class DocumentationSerialiser implements DocumentSerialiser2<DocumentationDoc> {

    private static final String TEXT = "txt";

    private final Serialiser2<DocumentationDoc> delegate;

    @Inject
    public DocumentationSerialiser(final Serialiser2Factory serialiser2Factory) {
        this.delegate = serialiser2Factory.createSerialiser(DocumentationDoc.class);
    }

    @Override
    public DocumentationDoc read(final Map<String, byte[]> data) throws IOException {
        final DocumentationDoc document = delegate.read(data);
        document.setData(EncodingUtil.asString(data.get(TEXT)));
        return document;
    }

    @Override
    public Map<String, byte[]> write(final DocumentationDoc document) throws IOException {
        final String text = document.getData();
        document.setData(null);

        final Map<String, byte[]> data = delegate.write(document);
        if (text != null) {
            data.put(TEXT, EncodingUtil.asBytes(text));
            document.setData(text);
        }

        return data;
    }
}
