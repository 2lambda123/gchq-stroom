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

package stroom.pipeline.cache;

import stroom.entity.event.EntityEvent;
import stroom.entity.event.EntityEventHandler;
import stroom.security.Security;
import stroom.cache.api.CacheManager;
import stroom.xmlschema.XmlSchemaCache;
import stroom.xmlschema.shared.XmlSchemaDoc;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@EntityEventHandler(type = XmlSchemaDoc.DOCUMENT_TYPE)
class SchemaPoolImpl extends AbstractPoolCache<SchemaKey, StoredSchema>
        implements SchemaPool, EntityEvent.Handler {
    private final SchemaLoader schemaLoader;
    private final Security security;

    @Inject
    SchemaPoolImpl(final CacheManager cacheManager,
                   final SchemaLoader schemaLoader,
                   final XmlSchemaCache xmlSchemaCache,
                   final Security security) {
        super(cacheManager, "Schema Pool");
        this.schemaLoader = schemaLoader;
        this.security = security;
        xmlSchemaCache.addClearHandler(this::clear);
    }

    @Override
    public PoolItem<StoredSchema> borrowObject(final SchemaKey key, final boolean usePool) {
        return internalBorrowObject(key, usePool);
    }

    @Override
    public void returnObject(final PoolItem<StoredSchema> poolItem, final boolean usePool) {
        internalReturnObject(poolItem, usePool);
    }

    @Override
    protected StoredSchema internalCreateValue(final Object key) {
        return security.asProcessingUserResult(() -> {
            final SchemaKey schemaKey = (SchemaKey) key;
            return schemaLoader.load(schemaKey.getSchemaLanguage(), schemaKey.getData(), schemaKey.getFindXMLSchemaCriteria());
        });
    }

    /**
     * We will clear the schema pool if there are any changes to any schemas.
     */
    @Override
    public void onChange(final EntityEvent event) {
        clear();
    }
}
