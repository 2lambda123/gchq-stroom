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

package stroom.data.store.impl.fs.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.alert.client.event.ConfirmEvent;
import stroom.data.store.impl.fs.shared.FsVolume;
import stroom.data.store.impl.fs.shared.FsVolume.VolumeUseStatus;
import stroom.data.store.impl.fs.shared.FsVolumeResource;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.index.shared.ValidationResult;
import stroom.item.client.SelectionBox;
import stroom.util.shared.ModelStringUtil;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Focus;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

import java.util.function.Consumer;

public class FSVolumeEditPresenter extends MyPresenterWidget<FSVolumeEditPresenter.VolumeEditView> {

    private static final FsVolumeResource FS_VOLUME_RESOURCE = GWT.create(FsVolumeResource.class);

    private final RestFactory restFactory;
    private FsVolume volume;

    @Inject
    public FSVolumeEditPresenter(final EventBus eventBus, final VolumeEditView view,
                                 final RestFactory restFactory) {
        super(eventBus, view);
        this.restFactory = restFactory;
    }

//    public void addVolume(final FsVolume volume, final Consumer<FsVolume> consumer) {
//        read(volume, "Add Volume", consumer);
//    }
//
//    public void editVolume(final FsVolume volume, final Consumer<FsVolume> consumer) {
//        read(volume, "Edit Volume", consumer);
//    }
//
//

    void show(final FsVolume volume, final String title, final Consumer<FsVolume> consumer) {
        read(volume);

        final PopupSize popupSize = PopupSize.resizableX();
        ShowPopupEvent.builder(this)
                .popupType(PopupType.OK_CANCEL_DIALOG)
                .popupSize(popupSize)
                .caption(title)
                .onShow(event -> getView().focus())
                .onHideRequest(event -> {
                    if (event.isOk()) {
                        write();
                        try {
                            if (volume.getId() == null) {
                                doWithVolumeValidation(volume, () -> createVolume(consumer, volume));
                            } else {
                                doWithVolumeValidation(volume, () -> updateVolume(consumer, volume));
                            }

                        } catch (final RuntimeException e) {
                            AlertEvent.fireError(FSVolumeEditPresenter.this, e.getMessage(), null);
                        }
                    } else {
                        consumer.accept(null);
                    }
                })
                .fire();
    }

    private void doWithVolumeValidation(final FsVolume volume,
                                        final Runnable work) {

        final Rest<ValidationResult> rest = restFactory.create();
        rest
                .onSuccess(validationResult -> {
                    if (validationResult.isOk()) {
                        if (work != null) {
                            work.run();
                        }
                    } else if (validationResult.isWarning()) {
                        ConfirmEvent.fireWarn(
                                FSVolumeEditPresenter.this,
                                validationResult.getMessage(),
                                confirmOk -> {
                                    if (confirmOk) {
                                        if (work != null) {
                                            work.run();
                                        }
                                    }
                                });
                    } else {
                        AlertEvent.fireError(
                                FSVolumeEditPresenter.this,
                                validationResult.getMessage(),
                                null);
                    }
                })
                .onFailure(throwable -> {
                    AlertEvent.fireError(FSVolumeEditPresenter.this, throwable.getMessage(), null);
                })
                .call(FS_VOLUME_RESOURCE)
                .validate(volume);
    }

    private void updateVolume(final Consumer<FsVolume> consumer, final FsVolume volume) {
        final Rest<FsVolume> rest = restFactory.create();
        rest
                .onSuccess(consumer)
                .call(FS_VOLUME_RESOURCE)
                .update(volume.getId(), volume);
    }

    private void createVolume(final Consumer<FsVolume> consumer, final FsVolume volume) {
        final Rest<FsVolume> rest = restFactory.create();
        rest
                .onSuccess(consumer)
                .call(FS_VOLUME_RESOURCE)
                .create(volume);
    }

    void hide() {
        HidePopupEvent.builder(this)
                .fire();
    }

    private void read(final FsVolume volume) {
        this.volume = volume;

        getView().getPath().setText(volume.getPath());
        getView().getStatus().addItems(VolumeUseStatus.values());
        getView().getStatus().setValue(volume.getStatus());

        if (volume.getByteLimit() != null) {
            getView().getByteLimit().setText(ModelStringUtil.formatIECByteSizeString(
                    volume.getByteLimit(),
                    true,
                    ModelStringUtil.DEFAULT_SIGNIFICANT_FIGURES));
        } else {
            getView().getByteLimit().setText("");
        }
    }

    private void write() {
        volume.setPath(getView().getPath().getText());
        volume.setStatus(getView().getStatus().getValue());

        Long bytesLimit = null;
        final String limit = getView().getByteLimit().getText().trim();
        if (limit.length() > 0) {
            bytesLimit = ModelStringUtil.parseIECByteSizeString(limit);
        }
        volume.setByteLimit(bytesLimit);
    }

    public interface VolumeEditView extends View, Focus {

        HasText getPath();

        SelectionBox<VolumeUseStatus> getStatus();

        HasText getByteLimit();
    }
}
