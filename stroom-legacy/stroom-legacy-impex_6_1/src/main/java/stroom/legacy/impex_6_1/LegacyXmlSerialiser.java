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

package stroom.legacy.impex_6_1;

import stroom.legacy.model_6_1.AuditedEntity;
import stroom.legacy.model_6_1.DocumentEntity;
import stroom.legacy.model_6_1.StreamType;
import stroom.legacy.model_6_1.XMLMarshallerUtil;
import stroom.util.shared.EntityServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

@Deprecated
public class LegacyXmlSerialiser {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyXmlSerialiser.class);

    private static final JAXBContext PIPELINEDATA_JAXB_CONTEXT = createJAXBContext(stroom.legacy.model_6_1.PipelineData.class);
    private static final JAXBContext DOCREF_JAXB_CONTEXT = createJAXBContext(stroom.legacy.model_6_1.DocRef.class);
    private static final JAXBContext INDEXFIELDS_JAXB_CONTEXT = createJAXBContext(stroom.legacy.model_6_1.IndexFields.class);
    private static final JAXBContext DOCREFS_JAXB_CONTEXT = createJAXBContext(stroom.legacy.model_6_1.DocRefs.class);

    public static <E extends DocumentEntity> void performImport(final E entity, final Map<String, byte[]> dataMap) {
        try {
            final List<Property> propertyList = BeanPropertyUtil.getPropertyList(entity.getClass(), false);

            final Config config = new Config();
            config.read(new StringReader(EncodingUtil.asString(dataMap.get("xml"))));

            // Output warnings where the config lists invalid properties.
            for (final String property : config.getProperties()) {
                boolean found = false;
                for (final Property prop : propertyList) {
                    if (prop.getName().equals(property)) {
                        found = true;
                        break;
                    }
                }
                if (DocumentEntity.AUDIT_FIELDS.contains(property)) {
                    found = true;
                }
            }

            // Only try and set valid properties.
            // Start with properties that are not being set from an external
            // file as the internal properties may affect the file extension of
            // the external files.
            for (final Property property : propertyList) {
                final String propertyName = property.getName();

                // Import non externalised properties.
                if (!property.isExternalFile()) {
                    // Ignore audit fields.
                    if (!DocumentEntity.AUDIT_FIELDS.contains(propertyName)) {
                        // Set the property if it is specified.
                        if (config.hasProperty(propertyName)) {
                            updateProperty(entity, property, config.get(propertyName));
                        }
                    }
                }
            }

            // Now set properties that are held in external files.
            for (final Property property : propertyList) {
                final String propertyName = property.getName();

                // Import the property from an external file if we are expected
                // to.
                if (property.isExternalFile()) {
                    final String fileExtension = property.getExtensionProvider().getExtension(entity, propertyName);
                    final String dataKey = propertyName + "." + fileExtension;
                    final String data = EncodingUtil.asString(dataMap.get(dataKey));
                    if (data != null) {
                        final List<Object> newDataList = new ArrayList<>();
                        newDataList.add(data);
                        updateProperty(entity, property, newDataList);
                    }
                }
            }
        } catch (final RuntimeException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private static void updateProperty(final Object object,
                                       final Property property,
                                       final List<Object> values) {
        try {
            if (AuditedEntity.class.isAssignableFrom(property.getType())) {
                AuditedEntity entity = (AuditedEntity) object;
                Object obj = (values != null && values.size() > 0)
                        ? values.iterator().next()
                        : null;

                if (obj instanceof String) {
                    final String value = (String) obj;
                    convertAndSetEntity(object, property, value);
                } else {
                    property.set(entity, null);
                }
            } else if (Set.class.isAssignableFrom(property.getType())) {
                final Set<AuditedEntity> newSet = new HashSet<>();
                property.set(object, newSet);

            } else {
                // Simple property
                if (values == null || values.size() == 0) {
                    property.set(object, null);
                } else {
                    final String value = values.get(0).toString();

                    if (String.class.equals(property.getType())) {
                        property.set(object, value);
                    } else if (Long.class.equals(property.getType())) {
                        property.set(object, Long.valueOf(value));
                    } else if (Integer.class.equals(property.getType())) {
                        property.set(object, Integer.valueOf(value));
                    } else if (Boolean.class.equals(property.getType())) {
                        property.set(object, Boolean.valueOf(value));
                    } else if (property.getType().isEnum()) {
                        property.set(object, Enum.valueOf((Class<Enum>) property.getType(), value));
                    } else if (property.getType().isPrimitive()) {
                        if (property.getType().getName().equals("boolean")) {
                            property.set(object, Boolean.valueOf(value));
                        } else if (property.getType().getName().equals("int")) {
                            property.set(object, Integer.valueOf(value));
                        } else if (property.getType().getName().equals("long")) {
                            property.set(object, Long.valueOf(value));
                        } else {
                            throw new EntityServiceException("Unexpected property type " + property.getType());
                        }
                    } else {
                        throw new EntityServiceException("Unexpected property type " + property.getType());
                    }
                }
            }
        } catch (final RuntimeException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static void convertAndSetEntity(final Object object,
                                            final Property property,
                                            final String value)
            throws InvocationTargetException, IllegalAccessException {

        if (StreamType.class.isAssignableFrom(property.getType())) {
            // Special case for streamType as the value in the xml is just the name
            // but the prop is a StreamType class

            final StreamType streamType;
            if (value != null) {
                streamType = new StreamType();
                streamType.setName(value);
            } else {
                streamType = null;
            }
            property.set(object, streamType);
        }
    }

    public static stroom.legacy.model_6_1.PipelineData getPipelineDataFromLegacyXml(final String xml) {
        if (xml != null) {
            try {
                return XMLMarshallerUtil.unmarshal(PIPELINEDATA_JAXB_CONTEXT,
                        stroom.legacy.model_6_1.PipelineData.class,
                        xml);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to unmarshal pipeline config", e);
            }
        }

        return null;
    }

    public static stroom.legacy.model_6_1.DocRef getDocRefFromLegacyXml(final String xml) {
        if (xml != null) {
            try {
                return XMLMarshallerUtil.unmarshal(DOCREF_JAXB_CONTEXT, stroom.legacy.model_6_1.DocRef.class, xml);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to unmarshal dashboard config", e);
            }
        }

        return null;
    }

    public static stroom.legacy.model_6_1.IndexFields getIndexFieldsFromLegacyXml(final String xml) {
        if (xml != null) {
            try {
                return stroom.util.xml.XMLMarshallerUtil.unmarshal(INDEXFIELDS_JAXB_CONTEXT,
                        stroom.legacy.model_6_1.IndexFields.class,
                        xml);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to unmarshal index fields", e);
            }
        }

        return null;
    }

    public static stroom.legacy.model_6_1.DocRefs getDocRefsFromLegacyXml(final String xml) {
        if (xml != null) {
            try {
                return stroom.util.xml.XMLMarshallerUtil.unmarshal(DOCREFS_JAXB_CONTEXT,
                        stroom.legacy.model_6_1.DocRefs.class,
                        xml);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to unmarshal docrefs", e);
            }
        }

        return null;
    }

    private static JAXBContext createJAXBContext(final Class<?> clazz) {
        try {
            return JAXBContext.newInstance(clazz);
        } catch (final JAXBException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
