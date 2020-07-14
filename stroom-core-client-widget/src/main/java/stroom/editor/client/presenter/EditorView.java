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

package stroom.editor.client.presenter;

import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.user.client.ui.HasText;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.View;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorTheme;
import stroom.editor.client.event.HasFormatHandlers;
import stroom.editor.client.view.IndicatorLines;
import stroom.util.shared.TextRange;
import stroom.widget.contextmenu.client.event.HasContextMenuHandlers;

import java.util.List;

public interface EditorView extends View, HasKeyDownHandlers, HasFormatHandlers, HasText, HasMouseDownHandlers,
        HasContextMenuHandlers, HasUiHandlers<EditorUiHandlers>, HasValueChangeHandlers<String> {
    void setText(String text);

    void setFirstLineNumber(int firstLineNumber);

    void setIndicators(IndicatorLines indicators);

    void setHighlights(List<TextRange> highlights);

    void setReadOnly(boolean readOnly);

    void setMode(AceEditorMode mode);

    void setTheme(AceEditorTheme theme);

    void format();

    Option getStylesOption();

    Option getLineNumbersOption();

    Option getIndicatorsOption();

    Option getLineWrapOption();

    void showFilterButton(boolean show);

    void setFilterActive(boolean active);

    void setControlsVisible(boolean visible);
}
