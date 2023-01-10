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

package stroom.security.client.presenter;

import stroom.security.client.presenter.GroupEditPresenter.UserGroupEditView;
import stroom.security.shared.User;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView;
import stroom.widget.popup.client.presenter.Size;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

public class GroupEditPresenter extends MyPresenterWidget<UserGroupEditView> {

    private final UserEditAddRemoveUsersPresenter addRemoveUsersPresenter;
    private final AppPermissionsPresenter appPermissionsPresenter;

    @Inject
    public GroupEditPresenter(final EventBus eventBus,
                              final UserGroupEditView view,
                              final UserEditAddRemoveUsersPresenter addRemoveUsersPresenter,
                              final AppPermissionsPresenter appPermissionsPresenter) {
        super(eventBus, view);
        this.addRemoveUsersPresenter = addRemoveUsersPresenter;
        this.appPermissionsPresenter = appPermissionsPresenter;

        view.setUsersView(addRemoveUsersPresenter.getView());
        view.setAppPermissionsView(appPermissionsPresenter.getView());
    }

    public void show(final User userRef, final PopupUiHandlers popupUiHandlers) {
        read(userRef);

        final PopupUiHandlers internalPopupUiHandlers = new PopupUiHandlers() {
            @Override
            public void onHideRequest(final boolean autoClose, final boolean ok) {
                HidePopupEvent.fire(GroupEditPresenter.this, GroupEditPresenter.this);
                popupUiHandlers.onHideRequest(autoClose, ok);
            }

            @Override
            public void onHide(final boolean autoClose, final boolean ok) {
                popupUiHandlers.onHide(autoClose, ok);
            }
        };
        final PopupSize popupSize = PopupSize.builder()
                .width(Size
                        .builder()
                        .initial(1000)
                        .min(1000)
                        .resizable(true)
                        .build())
                .height(Size
                        .builder()
                        .initial(555)
                        .min(555)
                        .resizable(true)
                        .build())
                .build();
        final String caption = "Group - " + userRef.getName();
        ShowPopupEvent.fire(
                GroupEditPresenter.this,
                GroupEditPresenter.this,
                PopupView.PopupType.CLOSE_DIALOG,
                popupSize,
                caption,
                internalPopupUiHandlers);
    }

    private void read(User userRef) {
        addRemoveUsersPresenter.setUser(userRef);
        appPermissionsPresenter.setUser(userRef);
    }

    public interface UserGroupEditView extends View {

        void setUsersView(View view);

        void setAppPermissionsView(View view);
    }
}
