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

package stroom.item.client;

import java.util.List;

import com.google.gwt.user.client.ui.ListBox;

public class StringListBox extends ListBox {
    public String getSelected() {
        if (getSelectedIndex() < 0) {
            return null;
        }

        return getItemText(getSelectedIndex());
    }

    public void setSelected(final String selected) {
        int index = -1;
        for (int i = 0; i < getItemCount() && index == -1; i++) {
            if (getItemText(i).equals(selected)) {
                index = i;
            }
        }

        setSelectedIndex(index);
    }

    public void addItems(final List<String> items) {
        for (final String item : items) {
            addItem(item);
        }
    }
}
