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

package stroom.processor.impl.db;

import stroom.util.xml.XMLMarshallerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBContext;

abstract class AbstractEntityMarshaller<T_ENTITY, T_OBJECT> implements Marshaller<T_ENTITY, T_OBJECT> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntityMarshaller.class);

    private final JAXBContext jaxbContext;

    public AbstractEntityMarshaller(final JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    @Override
    public T_ENTITY marshal(final T_ENTITY entity) {
        try {
            Object object = getObject(entity);

            // Strip out references to empty collections.
            try {
                object = XMLMarshallerUtil.removeEmptyCollections(object);
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }

            final String data = XMLMarshallerUtil.marshal(jaxbContext, object);
            setData(entity, data);
        } catch (final RuntimeException e) {
            LOGGER.debug("Problem marshaling {} {}", new Object[]{entity.getClass(), entity}, e);
            LOGGER.warn("Problem marshaling {} {} - {} (enable debug for full trace)",
                    entity.getClass(), entity, String.valueOf(e));
        }
        return entity;
    }

    @Override
    public T_ENTITY unmarshal(final T_ENTITY entity) {
        try {
            final String data = getData(entity);
            final T_OBJECT object = XMLMarshallerUtil.unmarshal(jaxbContext, getObjectType(), data);
            setObject(entity, object);
        } catch (final RuntimeException e) {
            LOGGER.debug("Unable to unmarshal entity!", e);
            LOGGER.warn(e.getMessage());
        }
        return entity;
    }

    protected abstract String getData(T_ENTITY entity);

    protected abstract void setData(T_ENTITY entity, String data);

    protected abstract Class<T_OBJECT> getObjectType();

    protected abstract String getEntityType();
}
