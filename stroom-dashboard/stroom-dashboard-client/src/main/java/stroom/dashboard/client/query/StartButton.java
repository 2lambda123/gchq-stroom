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

package stroom.dashboard.client.query;

import stroom.widget.button.client.FabButton;

public class StartButton extends FabButton {

    public StartButton() {
        searchMode();
    }

    public void searchMode() {
        addStyleName("stroom-dashboard-query-play");
        removeStyleName("stroom-dashboard-query-pause");
        setIconClassName("stroom-dashboard-query-play-icon");
        setTitle("Execute Query");
    }

    public void pauseMode() {
        addStyleName("stroom-dashboard-query-pause");
        removeStyleName("stroom-dashboard-query-play");
        setIconClassName("stroom-dashboard-query-pause-icon");
        setTitle("Pause Query");
    }

    public void resumeMode() {
        addStyleName("stroom-dashboard-query-play");
        removeStyleName("stroom-dashboard-query-pause");
        setIconClassName("stroom-dashboard-query-play-icon");
        setTitle("Resume Query");
    }
}
