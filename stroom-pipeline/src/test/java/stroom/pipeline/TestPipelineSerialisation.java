/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.pipeline;


import stroom.pipeline.shared.data.PipelineData;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.util.xml.XMLMarshallerUtil;

import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;

import static org.assertj.core.api.Assertions.assertThat;

class TestPipelineSerialisation {

    private static final PipelineElementType ELEM_TYPE = new PipelineElementType("TestElement", null,
            new String[]{PipelineElementType.ROLE_TARGET, PipelineElementType.ROLE_HAS_TARGETS}, null);

    @Test
    void testEmpty() {
        final JAXBContext jaxbContext = PipelineSerialiser.getJAXBContext();
        final PipelineData pipelineData = new PipelineData();
        final String string = XMLMarshallerUtil.marshal(jaxbContext,
                XMLMarshallerUtil.removeEmptyCollections(pipelineData));
        assertThat("<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" +
                "<pipeline/>").isEqualTo(string.trim());
    }

    @Test
    void testElements() {
        final JAXBContext jaxbContext = PipelineSerialiser.getJAXBContext();
        final PipelineData pipelineData = new PipelineData();
        pipelineData.addElement(ELEM_TYPE, "test1");
        final String string = XMLMarshallerUtil.marshal(jaxbContext,
                XMLMarshallerUtil.removeEmptyCollections(pipelineData));
        assertThat("<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" +
                "<pipeline>\n" +
                "   <elements>\n" +
                "      <add>\n" +
                "         <element>\n" +
                "            <id>test1</id>\n" +
                "            <type>TestElement</type>\n" +
                "         </element>\n" +
                "      </add>\n" +
                "   </elements>\n" +
                "</pipeline>").isEqualTo(string.trim());
    }
}
