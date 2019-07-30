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

package stroom.task.client.presenter;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;
import stroom.task.client.presenter.UserTaskPresenter.UserTaskView;
import stroom.task.shared.TaskProgress;
import stroom.util.shared.ModelStringUtil;
import stroom.task.shared.TaskId;

public class UserTaskPresenter extends MyPresenterWidget<UserTaskView> {
    @Inject
    public UserTaskPresenter(final EventBus eventBus, final UserTaskView view) {
        super(eventBus, view);
    }

    public void setTaskProgress(final TaskProgress taskProgress) {
        getView().setTaskName(taskProgress.getTaskName());
        getView().getTaskAge().setText(ModelStringUtil.formatDurationString(taskProgress.getAgeMs()));
        getView().setId(taskProgress.getId());
        getView().getTaskStatus().setText(taskProgress.getTaskInfo());
    }

    public void setTerminateVisible(final boolean visible) {
        getView().setTerminateVisible(visible);
    }

    public void setUiHandlers(final UserTaskUiHandlers uiHandlers) {
        getView().setUiHandlers(uiHandlers);
    }

    public interface UserTaskView extends View, HasUiHandlers<UserTaskUiHandlers> {
        void setTaskName(String taskName);

        HasText getTaskAge();

        HasText getTaskStatus();

        void setId(TaskId id);

        void setTerminateVisible(boolean visible);
    }

    public interface Resources extends ClientBundle {
        ImageResource terminate();
    }
}
