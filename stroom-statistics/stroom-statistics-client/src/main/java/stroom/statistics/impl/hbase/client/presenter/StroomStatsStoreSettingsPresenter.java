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

package stroom.statistics.impl.hbase.client.presenter;

import stroom.core.client.event.DirtyKeyDownHander;
import stroom.docref.DocRef;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.entity.client.presenter.HasWrite;
import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.statistics.impl.hbase.shared.EventStoreTimeIntervalEnum;
import stroom.statistics.impl.hbase.shared.StatisticRollUpType;
import stroom.statistics.impl.hbase.shared.StatisticType;
import stroom.statistics.impl.hbase.shared.StroomStatsStoreDoc;
import stroom.widget.tickbox.client.view.CustomCheckBox;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.TextArea;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

public class StroomStatsStoreSettingsPresenter
        extends MyPresenterWidget<StroomStatsStoreSettingsPresenter.StroomStatsStoreSettingsView>
        implements HasDocumentRead<StroomStatsStoreDoc>, HasWrite<StroomStatsStoreDoc>, HasDirtyHandlers,
        ReadOnlyChangeHandler,
        StroomStatsStoreSettingsUiHandlers {

    @Inject
    public StroomStatsStoreSettingsPresenter(
            final EventBus eventBus,
            final StroomStatsStoreSettingsPresenter.StroomStatsStoreSettingsView view) {

        super(eventBus, view);

        final KeyDownHandler keyDownHander = new DirtyKeyDownHander() {
            @Override
            public void onDirty(final KeyDownEvent event) {
                DirtyEvent.fire(StroomStatsStoreSettingsPresenter.this, true);
            }
        };

        registerHandler(view.getDescription().addKeyDownHandler(keyDownHander));

        view.setUiHandlers(this);
    }

    @Override
    public void onChange() {
        DirtyEvent.fire(StroomStatsStoreSettingsPresenter.this, true);
    }

    @Override
    public void read(final DocRef docRef, final StroomStatsStoreDoc stroomStatsStoreEntity) {
        if (stroomStatsStoreEntity != null) {
            getView().getDescription().setText(stroomStatsStoreEntity.getDescription());
            getView().setStatisticType(stroomStatsStoreEntity.getStatisticType());
            getView().getEnabled().setValue(stroomStatsStoreEntity.isEnabled());
            getView().setPrecision(stroomStatsStoreEntity.getPrecision());
            getView().setRollUpType(stroomStatsStoreEntity.getRollUpType());
        }
    }

    @Override
    public StroomStatsStoreDoc write(final StroomStatsStoreDoc stroomStatsStoreEntity) {
        if (stroomStatsStoreEntity != null) {
            stroomStatsStoreEntity.setDescription(getView().getDescription().getText());
            stroomStatsStoreEntity.setStatisticType(getView().getStatisticType());
            stroomStatsStoreEntity.setEnabled(getView().getEnabled().getValue());
            stroomStatsStoreEntity.setPrecision(getView().getPrecision());
            stroomStatsStoreEntity.setRollUpType(getView().getRollUpType());
        }
        return stroomStatsStoreEntity;
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        getView().onReadOnly(readOnly);
    }

    @Override
    public HandlerRegistration addDirtyHandler(final DirtyHandler handler) {
        return addHandlerToSource(DirtyEvent.getType(), handler);
    }

    public interface StroomStatsStoreSettingsView
            extends View, HasUiHandlers<StroomStatsStoreSettingsUiHandlers>, ReadOnlyChangeHandler {

        TextArea getDescription();

        StatisticType getStatisticType();

        void setStatisticType(StatisticType statisticType);

        StatisticRollUpType getRollUpType();

        void setRollUpType(StatisticRollUpType statisticRollUpType);

        EventStoreTimeIntervalEnum getPrecision();

        void setPrecision(EventStoreTimeIntervalEnum precision);

        CustomCheckBox getEnabled();
    }
}
