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
 *
 */

package stroom.index;


import stroom.docref.DocRef;
import stroom.index.impl.IndexSerialiser;
import stroom.index.impl.IndexStore;
import stroom.index.shared.LuceneIndexDoc;
import stroom.index.shared.LuceneIndexField;
import stroom.index.impl.IndexFields;
import stroom.legacy.impex_6_1.LegacyIndexDeserialiser;
import stroom.legacy.impex_6_1.LegacyXmlSerialiser;
import stroom.legacy.impex_6_1.MappingUtil;
import stroom.test.AbstractCoreIntegrationTest;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestIndexStoreImpl extends AbstractCoreIntegrationTest {

    @Inject
    private IndexStore indexStore;
    @Inject
    private IndexSerialiser indexSerialiser;
    @Inject
    private LegacyIndexDeserialiser legacyIndexDeserialiser;

    private DocRef testIndex;
    private DocRef refIndex;

    @BeforeEach
    void setup() {
        refIndex = indexStore.createDocument("Ref index");
        testIndex = indexStore.createDocument("Test index");

        final List<LuceneIndexField> indexFields = IndexFields.createStreamIndexFields();
        indexFields.add(LuceneIndexField.createDateField("TimeCreated"));
        indexFields.add(LuceneIndexField.createField("User"));

        final LuceneIndexDoc index = indexStore.readDocument(testIndex);
        index.setFields(indexFields);
        indexStore.writeDocument(index);
    }

    @Test
    void testIndexRetrieval() {
        List<DocRef> list = indexStore.list();
        assertThat(list.size()).isEqualTo(2);

        assertThat(list.stream()
                .filter(docRef ->
                        docRef.getName().equals("Test index"))
                .count())
                .isEqualTo(1);
        assertThat((int) list.stream()
                .filter(docRef ->
                        docRef.getName().equals("Ref index"))
                .count())
                .isEqualTo(1);

        final LuceneIndexDoc index = indexStore.readDocument(list.stream()
                .filter(docRef ->
                        docRef.getName().equals("Test index"))
                .findFirst()
                .get());

        assertThat(index).isNotNull();
        assertThat(index.getName()).isEqualTo("Test index");

        final String xml = "" +
                "<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" +
                "<fields>\n" +
                "   <field>\n" +
                "      <analyzerType>KEYWORD</analyzerType>\n" +
                "      <caseSensitive>false</caseSensitive>\n" +
                "      <fieldName>StreamId</fieldName>\n" +
                "      <fieldType>ID</fieldType>\n" +
                "      <indexed>true</indexed>\n" +
                "      <stored>true</stored>\n" +
                "      <termPositions>false</termPositions>\n" +
                "   </field>\n" +
                "   <field>\n" +
                "      <analyzerType>KEYWORD</analyzerType>\n" +
                "      <caseSensitive>false</caseSensitive>\n" +
                "      <fieldName>EventId</fieldName>\n" +
                "      <fieldType>ID</fieldType>\n" +
                "      <indexed>true</indexed>\n" +
                "      <stored>true</stored>\n" +
                "      <termPositions>false</termPositions>\n" +
                "   </field>\n" +
                "   <field>\n" +
                "      <analyzerType>ALPHA_NUMERIC</analyzerType>\n" +
                "      <caseSensitive>false</caseSensitive>\n" +
                "      <fieldName>TimeCreated</fieldName>\n" +
                "      <fieldType>DATE_FIELD</fieldType>\n" +
                "      <indexed>true</indexed>\n" +
                "      <stored>false</stored>\n" +
                "      <termPositions>false</termPositions>\n" +
                "   </field>\n" +
                "   <field>\n" +
                "      <analyzerType>ALPHA_NUMERIC</analyzerType>\n" +
                "      <caseSensitive>false</caseSensitive>\n" +
                "      <fieldName>User</fieldName>\n" +
                "      <fieldType>FIELD</fieldType>\n" +
                "      <indexed>true</indexed>\n" +
                "      <stored>false</stored>\n" +
                "      <termPositions>false</termPositions>\n" +
                "   </field>\n" +
                "</fields>\n";
        final List<LuceneIndexField> indexFields = MappingUtil.map(LegacyXmlSerialiser.getIndexFieldsFromLegacyXml(xml));
        assertThat(index.getFields()).isEqualTo(indexFields);
    }

    @Test
    void testLoad() {
        LuceneIndexDoc index = indexStore.readDocument(testIndex);
        assertThat(index).isNotNull();
        assertThat(index.getName()).isEqualTo("Test index");
    }

    @Test
    void testClientSideStuff1() {
        LuceneIndexDoc index = indexStore.readDocument(refIndex);
        indexStore.writeDocument(index);

    }

    @Test
    void testClientSideStuff2() {
        LuceneIndexDoc index = indexStore.readDocument(testIndex);
        indexStore.writeDocument(index);
    }
}
