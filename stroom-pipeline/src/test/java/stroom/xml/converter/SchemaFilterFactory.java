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

package stroom.xml.converter;

import stroom.cache.MockSchemaPool;
import stroom.pipeline.cache.SchemaLoaderImpl;
import stroom.pipeline.cache.SchemaPool;
import stroom.docstore.Persistence;
import stroom.docstore.impl.Serialiser2FactoryImpl;
import stroom.docstore.impl.StoreFactoryImpl;
import stroom.docstore.impl.memory.MemoryPersistence;
import stroom.pipeline.LocationFactoryProxy;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.filter.SchemaFilter;
import stroom.pipeline.state.PipelineContext;
import stroom.security.impl.mock.MockSecurityContext;
import stroom.security.SecurityContext;
import stroom.xml.converter.ds3.DS3ParserFactory;
import stroom.pipeline.xmlschema.XmlSchemaCache;
import stroom.pipeline.xmlschema.XmlSchemaSerialiser;
import stroom.pipeline.xmlschema.XmlSchemaStore;
import stroom.pipeline.xmlschema.XmlSchemaStoreImpl;
import stroom.xmlschema.shared.FindXMLSchemaCriteria;

public class SchemaFilterFactory {
    private final SecurityContext securityContext = new MockSecurityContext();
    private final Persistence persistence = new MemoryPersistence();
    private final XmlSchemaSerialiser serialiser = new XmlSchemaSerialiser(new Serialiser2FactoryImpl());
    private final XmlSchemaStore xmlSchemaStore = new XmlSchemaStoreImpl(
            new StoreFactoryImpl(securityContext, persistence), securityContext, persistence, serialiser);
    private final XmlSchemaCache xmlSchemaCache = new XmlSchemaCache(xmlSchemaStore);
    private final SchemaLoaderImpl schemaLoader = new SchemaLoaderImpl(xmlSchemaCache);

    public SchemaFilterFactory() {
        // loadXMLSchema(DataSplitterParserFactory.SCHEMA_GROUP,
        // DataSplitterParserFactory.SCHEMA_NAME,
        // DataSplitterParserFactory.NAMESPACE_URI,
        // DataSplitterParserFactory.SYSTEM_ID,
        // "data-splitter/v1.4/data-splitter-v1.4.xsd");
        // loadXMLSchema(DS2ParserFactory.SCHEMA_GROUP,
        // DS2ParserFactory.SCHEMA_NAME, DS2ParserFactory.NAMESPACE_URI,
        // DS2ParserFactory.SYSTEM_ID,
        // "data-splitter/v2.0/data-splitter-v2.0.xsd");
        loadXMLSchema(DS3ParserFactory.SCHEMA_GROUP, DS3ParserFactory.SCHEMA_NAME, DS3ParserFactory.NAMESPACE_URI,
                DS3ParserFactory.SYSTEM_ID, "data-splitter/v3.0/data-splitter-v3.0.xsd");
    }

    public SchemaFilter getSchemaFilter(final String namespaceURI, final ErrorReceiverProxy errorReceiverProxy) {
        final SchemaPool schemaPool = new MockSchemaPool(schemaLoader);
        final FindXMLSchemaCriteria schemaConstraint = new FindXMLSchemaCriteria();
        schemaConstraint.setNamespaceURI(namespaceURI);

        final SchemaFilter schemaFilter = new SchemaFilter(schemaPool, xmlSchemaCache, errorReceiverProxy,
                new LocationFactoryProxy(), new PipelineContext());
        schemaFilter.setSchemaConstraint(schemaConstraint);
        schemaFilter.setUseOriginalLocator(true);

        return schemaFilter;
    }

    private void loadXMLSchema(final String schemaGroup, final String schemaName, final String namespaceURI,
                               final String systemId, final String fileName) {
//        final Path dir = FileSystemTestUtil.getConfigXSDDir();
//
//        final Path file = dir.resolve(fileName);
//
//        final DocRef docRef = xmlSchemaStore.createDocument(schemaName);
//        final XmlSchemaDoc xmlSchema = xmlSchemaStore.readDocument(docRef);
//        xmlSchema.setSchemaGroup(schemaGroup);
//        xmlSchema.setName(schemaName);
//        xmlSchema.setNamespaceURI(namespaceURI);
//        xmlSchema.setSystemId(systemId);
//        xmlSchema.setData(StreamUtil.fileToString(file));
//        xmlSchemaStore.writeDocument(xmlSchema);
    }
}
