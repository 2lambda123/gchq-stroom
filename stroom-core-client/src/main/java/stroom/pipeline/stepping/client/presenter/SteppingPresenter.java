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

package stroom.pipeline.stepping.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.data.client.presenter.ClassificationUiHandlers;
import stroom.data.client.presenter.SourcePresenter;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.editor.client.view.IndicatorLines;
import stroom.meta.shared.FindMetaCriteria;
import stroom.meta.shared.Meta;
import stroom.pipeline.shared.PipelineModelException;
import stroom.pipeline.shared.PipelineResource;
import stroom.pipeline.shared.SharedElementData;
import stroom.pipeline.shared.SourceLocation;
import stroom.pipeline.shared.data.PipelineData;
import stroom.pipeline.shared.data.PipelineElement;
import stroom.pipeline.shared.data.PipelineProperty;
import stroom.pipeline.shared.stepping.PipelineStepRequest;
import stroom.pipeline.shared.stepping.StepLocation;
import stroom.pipeline.shared.stepping.StepType;
import stroom.pipeline.shared.stepping.SteppingResource;
import stroom.pipeline.shared.stepping.SteppingResult;
import stroom.pipeline.structure.client.presenter.PipelineModel;
import stroom.pipeline.structure.client.presenter.PipelineTreePresenter;
import stroom.svg.client.Preset;
import stroom.svg.client.SvgPresets;
import stroom.task.client.TaskEndEvent;
import stroom.task.client.TaskStartEvent;
import stroom.util.shared.Indicators;
import stroom.widget.button.client.ButtonPanel;
import stroom.widget.button.client.ButtonView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.Layer;
import com.gwtplatform.mvp.client.LayerContainer;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SteppingPresenter extends MyPresenterWidget<SteppingPresenter.SteppingView> implements HasDirtyHandlers {

    private static final PipelineResource PIPELINE_RESOURCE = GWT.create(PipelineResource.class);
    private static final SteppingResource STEPPING_RESOURCE = GWT.create(SteppingResource.class);

    private final PipelineStepRequest request;
    private final PipelineTreePresenter pipelineTreePresenter;
    private final SourcePresenter sourcePresenter;
    private final Provider<ElementPresenter> editorProvider;
    private final StepLocationPresenter stepLocationPresenter;
    private final StepControlPresenter stepControlPresenter;
    private final RestFactory restFactory;
    private final Map<String, ElementPresenter> editorMap = new HashMap<>();
    private final PipelineModel pipelineModel;
    private final ButtonView saveButton;
    private boolean foundRecord;
    private boolean showingData;
    private boolean busyTranslating;
    private SteppingResult lastFoundResult;
    private SteppingResult currentResult;
    private ButtonPanel leftButtons;

    private Meta meta;

    @Inject
    public SteppingPresenter(final EventBus eventBus, final SteppingView view,
                             final PipelineTreePresenter pipelineTreePresenter,
                             final RestFactory restFactory,
                             final SourcePresenter sourcePresenter,
                             final Provider<ElementPresenter> editorProvider,
                             final StepLocationPresenter stepLocationPresenter,
                             final StepControlPresenter stepControlPresenter) {
        super(eventBus, view);
        this.restFactory = restFactory;

        this.pipelineTreePresenter = pipelineTreePresenter;
        this.sourcePresenter = sourcePresenter;
        this.editorProvider = editorProvider;
        this.stepLocationPresenter = stepLocationPresenter;
        this.stepControlPresenter = stepControlPresenter;

        view.addWidgetRight(stepLocationPresenter.getView().asWidget());
        view.addWidgetRight(stepControlPresenter.getView().asWidget());
        view.setTreeView(pipelineTreePresenter.getView());

        sourcePresenter.setSteppingSource(true);

        pipelineModel = new PipelineModel();
        pipelineTreePresenter.setModel(pipelineModel);
        pipelineTreePresenter.setPipelineTreeBuilder(new SteppingPipelineTreeBuilder());
        pipelineTreePresenter.setAllowNullSelection(false);

        // Create the translation request to use.
        request = new PipelineStepRequest();

        stepControlPresenter.setEnabledButtons(
                false,
                request.getStepType(),
                true,
                showingData,
                foundRecord);

        saveButton = addButtonLeft(SvgPresets.SAVE);
    }

    @Override
    protected void onBind() {
        registerHandler(
                pipelineTreePresenter.getSelectionModel().addSelectionChangeHandler(event -> {
                    final PipelineElement selectedElement = pipelineTreePresenter.getSelectionModel()
                            .getSelectedObject();
                    onSelect(selectedElement);
                }));
        registerHandler(stepLocationPresenter.addStepControlHandler(event ->
                step(event.getStepType(), event.getStepLocation())));
        registerHandler(stepControlPresenter.addStepControlHandler(event ->
                step(event.getStepType(), event.getStepLocation())));
        registerHandler(saveButton.addClickHandler(event -> save()));
    }

    private ButtonView addButtonLeft(final Preset preset) {
        if (leftButtons == null) {
            leftButtons = new ButtonPanel();
            getView().addWidgetLeft(leftButtons);
        }

        return leftButtons.addButton(preset);
    }

    private PresenterWidget<?> getContent(final PipelineElement element) {
        if (PipelineModel.SOURCE_ELEMENT.getElementType().equals(element.getElementType())) {
            return sourcePresenter;

        } else {
            final String elementId = element.getId();
            ElementPresenter editorPresenter = editorMap.get(elementId);
            if (editorPresenter == null) {
                final DirtyHandler dirtyEditorHandler = event -> {
                    DirtyEvent.fire(SteppingPresenter.this, true);
                    saveButton.setEnabled(true);
                };

                final List<PipelineProperty> properties = pipelineModel.getProperties(element);

                final ElementPresenter presenter = editorProvider.get();
                presenter.setElement(element);
                presenter.setProperties(properties);
                presenter.setFeedName(meta.getFeedName());
                presenter.setPipelineName(request.getPipeline().getName());
                presenter.setPipelineStepRequest(request);
                editorMap.put(elementId, presenter);
                presenter.addDirtyHandler(dirtyEditorHandler);

                editorPresenter = presenter;
            }

            // Refresh this editor if it needs it.
            refreshEditor(editorPresenter, elementId);

            return editorPresenter;
        }
    }

    private void refreshEditor(final ElementPresenter editorPresenter, final String elementId) {
        editorPresenter.load()
                .onSuccess(result -> {
                    if (editorPresenter.isRefreshRequired()) {
                        editorPresenter.setRefreshRequired(false);

                        // Update code pane.
                        refreshEditorCodeIndicators(editorPresenter, elementId);

                        // Update IO data.
                        refreshEditorIO(editorPresenter, elementId);
                    }
                })
                .onFailure(throwable -> AlertEvent.fireError(this, throwable.getMessage(), null));
    }

    private void refreshEditorCodeIndicators(final ElementPresenter editorPresenter, final String elementId) {
        // Only update the code indicators if we have a current result.
        if (currentResult != null && currentResult.getStepData() != null) {
            final SharedElementData elementData = currentResult.getStepData().getElementData(elementId);
            if (elementData != null) {
                final Indicators codeIndicators = elementData.getCodeIndicators();
                // Always set the indicators for the code pane as errors in the
                // code pane could be responsible for no record being found.
                editorPresenter.setCodeIndicators(new IndicatorLines(codeIndicators));
            }
        }
    }

    private void refreshEditorIO(final ElementPresenter editorPresenter, final String elementId) {
        // Only update the input/output if we found a record.
        if (lastFoundResult != null) {
            final SharedElementData elementData = lastFoundResult.getStepData().getElementData(elementId);
            if (elementData != null) {
                final Indicators outputIndicators = elementData.getOutputIndicators();
                final String input = notNull(elementData.getInput());
                final String output = notNull(elementData.getOutput());

                editorPresenter.setInput(input, 1, elementData.isFormatInput(), null);

                if (output.length() == 0 && outputIndicators != null && outputIndicators.getMaxSeverity() != null) {
                    editorPresenter.setOutput(
                            outputIndicators.toString(),
                            1,
                            false,
                            null);
                } else {
                    // Don't try and format text output.
                    editorPresenter.setOutput(
                            output,
                            1,
                            elementData.isFormatOutput(),
                            new IndicatorLines(outputIndicators));
                }
            } else {
                // // if we didn't find a record then it could be the input that
                // is
                // // responsible. Show any error that has been created..
                // if (inputIndicators != null && inputIndicators.hasSummary())
                // {
                // editorPresenter.setInputIndicators(inputIndicators);
                // } else if (outputIndicators != null
                // && outputIndicators.hasSummary()) {
                // editorPresenter.setOutputIndicators(outputIndicators);
                // }
            }
        }
    }

    public void read(final DocRef pipeline,
                     final StepType stepType,
                     final StepLocation stepLocation,
                     final Meta meta,
                     final String childStreamType) {
        this.meta = meta;

        // Load the stream.
        // When we start stepping we are not on a record so want to see
        // from the start of the stream for non-segmented with no highlight and
        // nothing for segmented. DataFetcher will interpret the -1 rec no to return
        // the right data.
        final SourceLocation sourceLocation = SourceLocation.builder(meta.getId())
                .withChildStreamType(childStreamType)
                .withPartIndex(stepLocation.getPartIndex())
                .withRecordIndex(Math.max(stepLocation.getRecordIndex(), 0))
                .build();
        sourcePresenter.setSourceLocation(sourceLocation);

        // Set the pipeline on the stepping action.
        request.setPipeline(pipeline);

        // Set the stream id on the stepping action.
        final FindMetaCriteria findMetaCriteria = FindMetaCriteria.createFromMeta(meta);
        request.setCriteria(findMetaCriteria);
        request.setChildStreamType(childStreamType);

        // Load the pipeline.
        final Rest<List<PipelineData>> rest = restFactory.create();
        rest
                .onSuccess(result -> {
                    final PipelineData pipelineData = result.get(result.size() - 1);
                    final List<PipelineData> baseStack = new ArrayList<>(result.size() - 1);

                    // If there is a stack of pipeline data then we need
                    // to make sure changes are reflected appropriately.
                    for (int i = 0; i < result.size() - 1; i++) {
                        baseStack.add(result.get(i));
                    }

                    try {
                        pipelineModel.setPipelineData(pipelineData);
                        pipelineModel.setBaseStack(baseStack);
                        pipelineModel.build();
                        pipelineTreePresenter.getSelectionModel()
                                .setSelected(PipelineModel.SOURCE_ELEMENT, true);

                        Scheduler.get().scheduleDeferred(() ->
                                getView().setTreeHeight(pipelineTreePresenter.getTreeHeight() + 3));
                    } catch (final PipelineModelException e) {
                        AlertEvent.fireError(SteppingPresenter.this, e.getMessage(), null);
                    }

                    if (stepType != null) {
                        step(stepType, new StepLocation(
                                meta.getId(),
                                stepLocation.getPartIndex(),
                                stepLocation.getRecordIndex()));
                    }
                })
                .call(PIPELINE_RESOURCE)
                .fetchPipelineData(pipeline);
    }

    public void save() {
        // Tell all editors to save.
        for (final Entry<String, ElementPresenter> entry : editorMap.entrySet()) {
            entry.getValue().save();
        }
        DirtyEvent.fire(this, false);
        saveButton.setEnabled(false);
    }

    private void step(final StepType stepType, final StepLocation stepLocation) {
        if (!busyTranslating) {
            busyTranslating = true;

            // If we are stepping to the first or last record then clear all
            // current state from the action.
            if (StepType.FIRST.equals(stepType) || StepType.LAST.equals(stepType)) {
                request.reset();
            }

            // Is the event telling us to jump to a specific location?
            if (stepLocation != null) {
                request.setStepLocation(stepLocation);
            }

            // Set dirty code on action.
            final Map<String, String> codeMap = new HashMap<>();
            for (final ElementPresenter editorPresenter : editorMap.values()) {
                if (editorPresenter.isDirtyCode()) {
                    final String elementId = editorPresenter.getElement().getId();
                    final String code = editorPresenter.getCode();
                    codeMap.put(elementId, code);
                }
            }
            request.setCode(codeMap);

            request.setStepType(stepType);

            final Rest<SteppingResult> rest = restFactory.create();
            rest
                    .onSuccess(this::readResult)
                    .onFailure(caught -> {
                        AlertEvent.fireErrorFromException(SteppingPresenter.this, caught, null);
                        busyTranslating = false;
                    })
                    .call(STEPPING_RESOURCE)
                    .step(request);
        }
    }

    private void readResult(final SteppingResult result) {
        try {
            currentResult = result;
            foundRecord = result.isFoundRecord();
            if (foundRecord) {
                showingData = true;
                lastFoundResult = result;
            }

            // Tell all editors that a refresh is required.
            for (final Entry<String, ElementPresenter> entry : editorMap.entrySet()) {
                entry.getValue().setRefreshRequired(true);
            }

            // Refresh the currently selected editor.
            final PipelineElement selectedElement = pipelineTreePresenter.getSelectionModel().getSelectedObject();
            if (selectedElement != null) {
                final String elementId = selectedElement.getId();
                final ElementPresenter editorPresenter = editorMap.get(elementId);
                if (editorPresenter != null) {
                    refreshEditor(editorPresenter, elementId);
                }
            }

            if (foundRecord) {
                // What we display depends on whether it is segmented (cooked) or not (raw)
                // Segmented shows one event segment on the screen with no highlighting
                // Non-segmented shows the record/event highlighted amongst a load of context.
                if (result.isSegmentedData()) {
                    // Strip any highlighting
                    final SourceLocation newSourceLocation = result.getStepData()
                            .getSourceLocation()
                            .copy()
                            .withHighlight(null)
                            .build();
                    sourcePresenter.setSourceLocation(newSourceLocation);
                } else {
                    sourcePresenter.setSourceLocationUsingHighlight(result.getStepData().getSourceLocation());
                }

                // We found a record so update the display to indicate the
                // record that was found and update the request with the new
                // position ready for the next step.
                request.setStepLocation(result.getStepLocation());
                stepLocationPresenter.setStepLocation(result.getStepLocation());
            }

            // Sync step filters.
            request.setStepFilterMap(result.getStepFilterMap());

            if (result.getGeneralErrors() != null && result.getGeneralErrors().size() > 0) {
                final StringBuilder sb = new StringBuilder();
                for (final String err : result.getGeneralErrors()) {
                    sb.append(err);
                    sb.append("\n");
                }

                AlertEvent.fireError(
                        this,
                        "Some errors occurred during stepping",
                        sb.toString(),
                        null);
            }

        } finally {
            stepControlPresenter.setEnabledButtons(
                    true,
                    request.getStepType(),
                    true,
                    showingData,
                    foundRecord);
            busyTranslating = false;
        }
    }

    /**
     * Ensures we don't set a null string into a field by returning an empty
     * string instead of null.
     */
    private String notNull(final String str) {
        if (str == null) {
            return "";
        }

        return str;
    }

    private void onSelect(final PipelineElement element) {
        if (element != null) {
            TaskStartEvent.fire(SteppingPresenter.this);
            Scheduler.get().scheduleDeferred(() -> {
                final PresenterWidget<?> content = getContent(element);
                if (content != null) {
                    // Set the content.
                    getView().getLayerContainer().show((Layer) content);
                }

                TaskEndEvent.fire(SteppingPresenter.this);
            });
        }
    }

    @Override
    public HandlerRegistration addDirtyHandler(final DirtyHandler handler) {
        return addHandlerToSource(DirtyEvent.getType(), handler);
    }

    public void setClassificationUiHandlers(final ClassificationUiHandlers classificationUiHandlers) {
        sourcePresenter.setClassificationUiHandlers(classificationUiHandlers);
    }

    public interface SteppingView extends View {

        void setTreeHeight(int height);

        void addWidgetLeft(Widget widget);

        void addWidgetRight(Widget widget);

        void setTreeView(View view);

        LayerContainer getLayerContainer();
    }
}
