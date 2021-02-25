/*
 * Copyright 2016 Crown Copyright
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

package stroom.dashboard.impl;

import stroom.query.api.v2.SearchRequest;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks serialisation by converting to and from the SearchRequest objects and comparing the results
 */

class TestSearchRequestSerialisation {

    private static ObjectMapper getMapper(final boolean indent) {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, indent);
        mapper.setSerializationInclusion(Include.NON_NULL);
        // Enabling default typing adds type information where it would otherwise be ambiguous, i.e. for
        // abstract classes
//        mapper.enableDefaultTyping();
        return mapper;
    }

    @Test
    void testJsonSearchRequestSerialisation() throws IOException {
        // Given
        SearchRequest searchRequest = SearchRequestTestData.apiSearchRequest();
        ObjectMapper objectMapper = getMapper(true);

        // When
        String serialisedSearchRequest = objectMapper.writeValueAsString(searchRequest);
        SearchRequest deserialisedSearchRequest = objectMapper.readValue(serialisedSearchRequest, SearchRequest.class);
        String reSerialisedSearchRequest = objectMapper.writeValueAsString(deserialisedSearchRequest);

        // Then
        assertThat(searchRequest).isEqualTo(deserialisedSearchRequest);
        assertThat(serialisedSearchRequest).isEqualTo(reSerialisedSearchRequest);
    }

//    @Test
//    void testXmlSearchRequestSerialisation() throws JAXBException {
//        // Given
//        SearchRequest searchRequest = SearchRequestTestData.apiSearchRequest();
//        final JAXBContext context = JAXBContext.newInstance(SearchRequest.class);
//        Marshaller marshaller = context.createMarshaller();
//        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//        Unmarshaller unmarshaller = context.createUnmarshaller();
//        ByteArrayOutputStream firstDeserialisation = new ByteArrayOutputStream();
//        ByteArrayOutputStream secondDeserialisation = new ByteArrayOutputStream();
//
//        // When
//        marshaller.marshal(searchRequest, firstDeserialisation);
//        String serialisedSearchRequest = firstDeserialisation.toString();
//        SearchRequest deserialisedSearchRequest = (SearchRequest) unmarshaller.unmarshal(
//                new ByteArrayInputStream(serialisedSearchRequest.getBytes()));
//        marshaller.marshal(deserialisedSearchRequest, secondDeserialisation);
//        String reSerialisedSearchRequest = secondDeserialisation.toString();
//
//        // Then
//        assertThat(searchRequest).isEqualTo(deserialisedSearchRequest);
//        assertThat(serialisedSearchRequest).isEqualTo(reSerialisedSearchRequest);
//    }

}
