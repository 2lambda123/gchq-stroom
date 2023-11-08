package stroom.pipeline;

import stroom.docstore.api.DocumentSerialiser2;
import stroom.docstore.api.Serialiser2;
import stroom.docstore.api.Serialiser2Factory;
import stroom.pipeline.shared.PipelineDoc;
import stroom.pipeline.shared.data.PipelineData;
import stroom.util.string.EncodingUtil;
import stroom.util.xml.XMLMarshallerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

public class PipelineSerialiser implements DocumentSerialiser2<PipelineDoc> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineSerialiser.class);

    private static final String XML = "xml";

    private final Serialiser2<PipelineDoc> delegate;
    private static JAXBContext jaxbContext;

    @Inject
    public PipelineSerialiser(final Serialiser2Factory serialiser2Factory) {
        this.delegate = serialiser2Factory.createSerialiser(PipelineDoc.class);
    }

    @Override
    public PipelineDoc read(final Map<String, byte[]> data) throws IOException {
        final PipelineDoc document = delegate.read(data);

        final String xml = EncodingUtil.asString(data.get(XML));
        final PipelineData pipelineData = getPipelineDataFromXml(xml);
        document.setPipelineData(pipelineData);

        return document;
    }

    @Override
    public Map<String, byte[]> write(final PipelineDoc document) throws IOException {
        PipelineData pipelineData = document.getPipelineData();
        document.setPipelineData(null);

        final Map<String, byte[]> data = delegate.write(document);

        // If the pipeline doesn't have data, it may be a new pipeline, create a blank one.
        if (pipelineData == null) {
            pipelineData = new PipelineData();
        }

        data.put(XML, EncodingUtil.asBytes(getXmlFromPipelineData(pipelineData)));

        document.setPipelineData(pipelineData);

        return data;
    }

//    public PipelineData getPipelineDataFromJson(final String json) throws IOException {
//        if (json != null) {
//            return mapper.readValue(new StringReader(json), PipelineData.class);
//        }
//        return null;
//    }
//
//    public String getXmlFromPipelineData(final PipelineData pipelineData) throws IOException {
//        if (pipelineData != null) {
//            final StringWriter stringWriter = new StringWriter();
//            mapper.writeValue(stringWriter, pipelineData);
//            return stringWriter.toString();
//        }
//        return null;
//    }

    public PipelineData getPipelineDataFromXml(final String xml) {
        if (xml != null) {
            try {
                return XMLMarshallerUtil.unmarshal(getJAXBContext(), PipelineData.class, xml);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to unmarshal pipeline config", e);
            }
        }

        return null;
    }

    public String getXmlFromPipelineData(final PipelineData pipelineData) {
        if (pipelineData != null) {
            try {
                return XMLMarshallerUtil.marshal(getJAXBContext(),
                        XMLMarshallerUtil.removeEmptyCollections(pipelineData));
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to marshal pipeline config", e);
            }
        }

        return null;
    }

    public static JAXBContext getJAXBContext() {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(PipelineData.class);
            } catch (final JAXBException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return jaxbContext;
    }
}
