/*
 * Copyright 2019 Crown Copyright
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

package stroom.kafka.client.presenter;

import stroom.core.client.event.DirtyKeyDownHander;
import stroom.docref.DocRef;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.kafka.client.presenter.KafkaConfigSettingsPresenter.KafkaConfigSettingsView;
import stroom.kafka.shared.KafkaConfigDoc;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.TextArea;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;

public class KafkaConfigSettingsPresenter extends DocumentEditPresenter<KafkaConfigSettingsView, KafkaConfigDoc> {

    @Inject
    public KafkaConfigSettingsPresenter(final EventBus eventBus, final KafkaConfigSettingsView view) {
        super(eventBus, view);

        // Add listeners for dirty events.
        final KeyDownHandler keyDownHander = new DirtyKeyDownHander() {
            @Override
            public void onDirty(final KeyDownEvent event) {
                setDirty(true);
            }
        };

        registerHandler(view.getDescription().addKeyDownHandler(keyDownHander));

    }

    @Override
    public String getType() {
        return KafkaConfigDoc.DOCUMENT_TYPE;
    }

    @Override
    protected void onRead(final DocRef docRef, final KafkaConfigDoc doc, final boolean readOnly) {
        getView().getDescription().setText(doc.getDescription());
    }

    @Override
    protected KafkaConfigDoc onWrite(final KafkaConfigDoc doc) {
        doc.setDescription(getView().getDescription().getText().trim());
        return doc;
    }

    public interface KafkaConfigSettingsView extends View {

        TextArea getDescription();
    }
}
