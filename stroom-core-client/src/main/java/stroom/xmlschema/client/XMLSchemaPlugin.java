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

package stroom.xmlschema.client;

import stroom.core.client.ContentManager;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.docstore.shared.DocRefUtil;
import stroom.document.client.DocumentPlugin;
import stroom.document.client.DocumentPluginEventManager;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.xmlschema.client.presenter.XMLSchemaPresenter;
import stroom.xmlschema.shared.XmlSchemaDoc;
import stroom.xmlschema.shared.XmlSchemaResource;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;

import java.util.function.Consumer;
import javax.inject.Singleton;

@Singleton
public class XMLSchemaPlugin extends DocumentPlugin<XmlSchemaDoc> {

    private static final XmlSchemaResource XML_SCHEMA_RESOURCE = GWT.create(XmlSchemaResource.class);

    private final Provider<XMLSchemaPresenter> editorProvider;
    private final RestFactory restFactory;

    @Inject
    public XMLSchemaPlugin(final EventBus eventBus,
                           final Provider<XMLSchemaPresenter> editorProvider,
                           final RestFactory restFactory,
                           final ContentManager contentManager,
                           final DocumentPluginEventManager entityPluginEventManager) {
        super(eventBus, contentManager, entityPluginEventManager);
        this.editorProvider = editorProvider;
        this.restFactory = restFactory;
    }

    @Override
    protected DocumentEditPresenter<?, ?> createEditor() {
        return editorProvider.get();
    }

    @Override
    public void load(final DocRef docRef,
                     final Consumer<XmlSchemaDoc> resultConsumer,
                     final Consumer<Throwable> errorConsumer) {
        final Rest<XmlSchemaDoc> rest = restFactory.create();
        rest
                .onSuccess(resultConsumer)
                .onFailure(errorConsumer)
                .call(XML_SCHEMA_RESOURCE)
                .fetch(docRef.getUuid());
    }

    @Override
    public void save(final DocRef docRef,
                     final XmlSchemaDoc document,
                     final Consumer<XmlSchemaDoc> resultConsumer,
                     final Consumer<Throwable> errorConsumer) {
        final Rest<XmlSchemaDoc> rest = restFactory.create();
        rest
                .onSuccess(resultConsumer)
                .onFailure(errorConsumer)
                .call(XML_SCHEMA_RESOURCE)
                .update(document.getUuid(), document);
    }

    @Override
    public String getType() {
        return XmlSchemaDoc.DOCUMENT_TYPE;
    }

    @Override
    protected DocRef getDocRef(final XmlSchemaDoc document) {
        return DocRefUtil.create(document);
    }
}
