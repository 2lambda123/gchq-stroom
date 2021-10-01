package stroom.data.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.data.client.presenter.CharacterNavigatorPresenter.CharacterNavigatorView;
import stroom.data.client.presenter.SourcePresenter.SourceView;
import stroom.data.client.presenter.TextPresenter.TextView;
import stroom.data.shared.DataResource;
import stroom.data.shared.DataType;
import stroom.data.shared.StreamTypeNames;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.pipeline.shared.AbstractFetchDataResult;
import stroom.pipeline.shared.FetchDataRequest;
import stroom.pipeline.shared.FetchDataResult;
import stroom.pipeline.shared.SourceLocation;
import stroom.pipeline.shared.stepping.StepLocation;
import stroom.pipeline.shared.stepping.StepType;
import stroom.pipeline.stepping.client.event.BeginPipelineSteppingEvent;
import stroom.security.client.api.ClientSecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.svg.client.Preset;
import stroom.ui.config.client.UiConfigCache;
import stroom.ui.config.shared.SourceConfig;
import stroom.util.shared.Count;
import stroom.util.shared.DataRange;
import stroom.util.shared.DefaultLocation;
import stroom.util.shared.HasCharacterData;
import stroom.util.shared.Location;
import stroom.util.shared.TextRange;
import stroom.widget.button.client.ButtonView;
import stroom.widget.progress.client.presenter.Progress;
import stroom.widget.progress.client.presenter.ProgressPresenter;
import stroom.widget.progress.client.presenter.ProgressPresenter.ProgressView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SourcePresenter extends MyPresenterWidget<SourceView> implements TextUiHandlers {

    private static final DataResource DATA_RESOURCE = GWT.create(DataResource.class);
    private static final int HIGHLIGHT_CONTEXT_CHARS_BEFORE = 1_500;
    private static final int HIGHLIGHT_CONTEXT_LINES_BEFORE = 4;

    private final ProgressPresenter progressPresenter;
    private final TextPresenter textPresenter;
    private final CharacterNavigatorPresenter characterNavigatorPresenter;
    //    private final Provider<SourceLocationPresenter> sourceLocationPresenterProvider;
    private final UiConfigCache uiConfigCache;
    private final RestFactory restFactory;
    private final ClientSecurityContext clientSecurityContext;
    private final DataNavigatorData dataNavigatorData;

    private SourceLocation requestedSourceLocation = null;
    private SourceLocation receivedSourceLocation = null;
    private FetchDataResult lastResult = null;
    private TextRange currentHighlight = null;
    private final int highlightDelta = 0;
    private ClassificationUiHandlers classificationUiHandlers;
    private boolean isSteppingSource = false;
    private Count<Long> exactCharCount = null;


    @Inject
    public SourcePresenter(final EventBus eventBus,
                           final SourceView view,
                           final ProgressPresenter progressPresenter,
                           final TextPresenter textPresenter,
                           final CharacterNavigatorPresenter characterNavigatorPresenter,
                           final UiConfigCache uiConfigCache,
                           final RestFactory restFactory,
                           final ClientSecurityContext clientSecurityContext) {
        super(eventBus, view);
        this.progressPresenter = progressPresenter;
        this.textPresenter = textPresenter;
        this.characterNavigatorPresenter = characterNavigatorPresenter;
        this.uiConfigCache = uiConfigCache;
        this.restFactory = restFactory;
        this.clientSecurityContext = clientSecurityContext;
        this.dataNavigatorData = new DataNavigatorData();

        setEditorOptions();

        view.setTextView(textPresenter.getView());
        view.setNavigatorView(characterNavigatorPresenter.getView());

        setupProgressBar(view, progressPresenter);

        textPresenter.setUiHandlers(this);

        characterNavigatorPresenter.setDisplay(dataNavigatorData);
    }

    private void setupProgressBar(final SourceView view,
                                  final ProgressPresenter progressPresenter) {
        view.setProgressView(progressPresenter.getView());
        progressPresenter.setVisible(false);

    }

    private void setEditorOptions() {
        textPresenter.setReadOnly(true);

        // Default to wrapped lines
        textPresenter.getLineWrapOption().setOn();
        textPresenter.getLineNumbersOption().setOn();
        textPresenter.getStylesOption().setOn();

        textPresenter.getUseVimBindingsOption().setAvailable();

        textPresenter.getBasicAutoCompletionOption().setUnavailable();
        textPresenter.getFormatAction().setUnavailable();
        textPresenter.getViewAsHexOption().setUnavailable();
    }

    private void updateStepControlVisibility() {
        final boolean hasStepPermission = clientSecurityContext.hasAppPermission(
                PermissionNames.STEPPING_PERMISSION);

        textPresenter.setControlsVisible(hasStepPermission && !isSteppingSource);
    }

    /**
     * Sets the source location/range according to the passed {@link SourceLocation}
     * If there is a highlight and it is outside the visible range then so be it.
     * Only re-fetches the data if the location/range has changed
     */
    public void setSourceLocation(final SourceLocation sourceLocation) {
        setSourceLocation(sourceLocation, false);
    }

    /**
     * Sets the source location/range according to the passed {@link SourceLocation}
     * If there is a highlight and it is outside the visible range then so be it.
     *
     * @param force If true forces a re-fetch of the data even if the location/range is
     *              the same as last time.
     */
    public void setSourceLocation(final SourceLocation sourceLocation, final boolean force) {
        updateStepControlVisibility();

        if (force || !Objects.equals(sourceLocation, requestedSourceLocation)) {
            // Keep a record of what data was asked for, which may differ from what we get back
            requestedSourceLocation = sourceLocation;

            doWithConfig(sourceConfig -> {
                fetchSource(sourceLocation, sourceConfig);
            });
        }
    }

    /**
     * Will attempt to set the source range using the passed highlight, i.e. if the highlight
     * is towards the end of the data then it will set the range to enclose the highlight.
     */
    public void setSourceLocationUsingHighlight(final SourceLocation sourceLocation) {
        currentHighlight = sourceLocation.getHighlight();
//        if (sourceLocation.getHighlight() == null || receivedSourceLocation == null) {
        if (sourceLocation.getHighlight() == null) {
            // no highlight so just get the requested data.
            setSourceLocation(sourceLocation);
        } else {
            updateStepControlVisibility();
            final TextRange highlight = sourceLocation.getHighlight();
            if (receivedSourceLocation != null && isCurrentSourceSuitable(sourceLocation)) {
                // The requested highlight is inside the currently held data so just update
                // the highlight in the editor
//                GWT.log("Using existing source");

                // Update the highlight in case refresh is called
                requestedSourceLocation = receivedSourceLocation.copy()
                        .withHighlight(highlight)
                        .build();

//                currentHighlight = highlight;
                updateEditorHighlights();
            } else {
                // Highlight is outside the currently held data so we need to fetch data
                // that contains the highlight.
                doWithConfig(sourceConfig -> {
                    final Location newSourceStart = buildNewSourceLocationFromHighlight(
                            sourceLocation, highlight, sourceConfig);
                    final SourceLocation newSourceLocation = sourceLocation.copy()
                            .withDataRange(DataRange.fromLocation(newSourceStart))
                            .build();

                    // Now fetch the required range
                    setSourceLocation(newSourceLocation, true);
                });
            }
        }
    }

    private Location buildNewSourceLocationFromHighlight(final SourceLocation sourceLocation,
                                                         final TextRange highlight,
                                                         final SourceConfig sourceConfig) {
        final Location newSourceStart;
        final Location highlightStart = highlight.getFrom();

        // If we are stepping backwards then this highlight will be before the last one we
        // requested. If we don't have previous data then treat it like stepping forward.
        final boolean isHighlightMovingBackwards = isHighlightMovingBackwards(
                sourceLocation,
                requestedSourceLocation);

        final Optional<Integer> optCurrLineCount = Optional.ofNullable(receivedSourceLocation)
                .flatMap(SourceLocation::getOptDataRange)
                .flatMap(DataRange::getLineCount);

        if (optCurrLineCount
                .filter(i -> i == 1)
                .isPresent()
                && highlight.isOnOneLine()
                && highlightStart.getColNo() > HIGHLIGHT_CONTEXT_CHARS_BEFORE) {

            // single line data and highlight
            final int newColNo;
            if (isHighlightMovingBackwards
                    && receivedSourceLocation.getDataRange() != null
                    && receivedSourceLocation.getDataRange().getOptLength().isPresent()) {
                // try and show just under a fetch's worth of data before
                final int highlightLen = highlight.getTo().getColNo() - highlight.getFrom().getColNo() + 1;
                newColNo = (int) (highlightStart.getColNo()
                        - sourceConfig.getMaxCharactersPerFetch()
                        + highlightLen
//                        - receivedSourceLocation.getDataRange().getLength()
                        + HIGHLIGHT_CONTEXT_CHARS_BEFORE);
            } else {
                // we need to change the visible range
                // to be some chars before the highlight to provide the user some context
                newColNo = highlightStart.getColNo() - HIGHLIGHT_CONTEXT_CHARS_BEFORE;
            }
            newSourceStart = DefaultLocation.of(1, Math.max(1, newColNo));
        } else if (highlightStart.getLineNo() > HIGHLIGHT_CONTEXT_LINES_BEFORE) {
            final int newLineNo;
            if (isHighlightMovingBackwards && optCurrLineCount.isPresent()) {
                // try and show just under a fetch's worth of data before
                newLineNo = highlightStart.getLineNo()
                        - optCurrLineCount.get()
                        + HIGHLIGHT_CONTEXT_LINES_BEFORE;
            } else {
                // Adjust the visible data range to be a few lines before the highlight
                // so the user has some context
                newLineNo = highlightStart.getLineNo() - HIGHLIGHT_CONTEXT_LINES_BEFORE;
            }
            newSourceStart = DefaultLocation.of(Math.max(1, newLineNo), 1);
        } else {
            // Shouldn't really come in here but just display from the start just in case
            newSourceStart = DefaultLocation.of(1, 1);
        }

//        GWT.log("Highlight: " + highlight.toString()
//                + " new start: " + newSourceStart.toString());
        return newSourceStart;
    }

    private boolean isHighlightMovingBackwards(final SourceLocation newSourceLocation,
                                               final SourceLocation oldSourceLocation) {
        if (newSourceLocation != null
                && newSourceLocation.getHighlight() != null
                && oldSourceLocation != null
                && oldSourceLocation.getHighlight() != null) {
            return newSourceLocation.getHighlight().isBefore(oldSourceLocation.getHighlight());
        } else {
            return false;
        }
    }

    private boolean isCurrentSourceSuitable(final SourceLocation sourceLocation) {
        final boolean result;
        if (receivedSourceLocation == null || receivedSourceLocation.getDataRange() == null) {
            result = false;
        } else {
            result = receivedSourceLocation.isSameSource(sourceLocation)
                    && sourceLocation.getHighlight().isInsideRange(
                    receivedSourceLocation.getDataRange().getLocationFrom(),
                    receivedSourceLocation.getDataRange().getLocationTo());

//            GWT.log("Highlight: " + sourceLocation.getHighlight().toString()
//                    + " isSameSource: " + receivedSourceLocation.isSameSource(sourceLocation)
//                    + " isInsideRange: " + sourceLocation.getHighlight().isInsideRange(
//                        receivedSourceLocation.getDataRange().getLocationFrom(),
//                        receivedSourceLocation.getDataRange().getLocationTo())
//                    + " received data: " + receivedSourceLocation.getDataRange().getLocationFrom().toString()
//                    + " => " + receivedSourceLocation.getDataRange().getLocationTo().toString()
//                    + " result: " + result);
        }
        return result;
    }

    public void setNavigatorControlsVisible(final boolean isVisible) {
        if (isVisible) {
            getView().setNavigatorView(characterNavigatorPresenter.getView());
        } else {
            getView().setNavigatorView(null);
        }
    }

    public void setSteppingSource(final boolean isSteppingSource) {
        this.isSteppingSource = isSteppingSource;
    }

    private void doWithConfig(final Consumer<SourceConfig> action) {
        uiConfigCache.get()
                .onSuccess(uiConfig ->
                        action.accept(uiConfig.getSource()))
                .onFailure(caught -> AlertEvent.fireError(
                        SourcePresenter.this,
                        caught.getMessage(),
                        null));
    }

    private void fetchSource(final SourceLocation sourceLocation,
                             final SourceConfig sourceConfig) {

        final SourceLocation.Builder builder = SourceLocation.builder(sourceLocation.getMetaId())
                .withPartIndex(sourceLocation.getPartIndex())
                .withRecordIndex(sourceLocation.getRecordIndex())
                .withDataRange(sourceLocation.getDataRange())
                .withHighlight(sourceLocation.getHighlight())
                .withChildStreamType(sourceLocation.getChildType());
        final FetchDataRequest request = new FetchDataRequest(builder.build());

        final Rest<AbstractFetchDataResult> rest = restFactory.create();

        rest
                .onSuccess(this::handleResponse)
                .onFailure(caught -> AlertEvent.fireError(
                        SourcePresenter.this,
                        caught.getMessage(),
                        null))
                .call(DATA_RESOURCE)
                .fetch(request);
    }

    private void handleResponse(final AbstractFetchDataResult result) {

        if (result instanceof FetchDataResult) {
            final FetchDataResult fetchDataResult = (FetchDataResult) result;
            receivedSourceLocation = result.getSourceLocation();

            if (receivedSourceLocation != null
                    && lastResult != null
                    && receivedSourceLocation.isSameSource(lastResult.getSourceLocation())) {
                // If we encounter an exact char count for this source then hold onto it
                // so we can still show it if we page backwards
                if (fetchDataResult.getTotalCharacterCount().isExact()
                        || exactCharCount == null) {
                    exactCharCount = fetchDataResult.getTotalCharacterCount();
                }
            } else {
                exactCharCount = null;
                currentHighlight = null;
            }
            // hold this separately as we may change the highlight without fetching new data
            currentHighlight = receivedSourceLocation != null
                    ? receivedSourceLocation.getHighlight()
                    : null;

            lastResult = fetchDataResult;
            setTitle(lastResult);
            classificationUiHandlers.setClassification(result.getClassification());

            updateEditor();
            updateNavigator(result);
            refreshProgressBar(true);
        } else {
            AlertEvent.fireError(
                    SourcePresenter.this,
                    "Unexpected type " + result.getClass().getName(),
                    null);
        }
    }

    private void refreshProgressBar(final boolean isVisible) {
        Progress progress = null;
        if (dataNavigatorData.isSegmented()
                && dataNavigatorData.getCharOffsetFrom().isPresent()
                && dataNavigatorData.getCharOffsetTo().isPresent()) {

            if (dataNavigatorData.getTotalChars().isExact()) {
                progress = Progress.boundedRange(
                        dataNavigatorData.getTotalChars().getCount() - 1, // count to zero based bound
                        dataNavigatorData.getCharOffsetFrom().get(),
                        dataNavigatorData.getCharOffsetTo().get());
            } else {
                progress = Progress.unboundedRange(
                        dataNavigatorData.getCharOffsetFrom().get(),
                        dataNavigatorData.getCharOffsetTo().get());
            }
        } else if (dataNavigatorData.getByteOffsetFrom().isPresent()
                && dataNavigatorData.getByteOffsetTo().isPresent()) {

            if (dataNavigatorData.getTotalBytes().isPresent()) {
                progress = Progress.boundedRange(
                        dataNavigatorData.getTotalBytes().get() - 1, // count to zero based bound
                        dataNavigatorData.getByteOffsetFrom().get(),
                        dataNavigatorData.getByteOffsetTo().get());
            } else {
                progress = Progress.unboundedRange(
                        dataNavigatorData.getByteOffsetFrom().get(),
                        dataNavigatorData.getByteOffsetTo().get());
            }
        }

        if (progress != null) {
            progressPresenter.setVisible(true);
            progressPresenter.setProgress(progress);

            if (progress.isComplete()) {
                // Don't want users clicking if we are showing everything
                progressPresenter.setClickHandler(null);
            } else {
                progressPresenter.setClickHandler(byteOffsetDbl -> {
                    final long byteOffset = (long) Math.floor(byteOffsetDbl);
                    // update the location with the new range
                    doWithConfig(sourceConfig -> {
                        final long maxChars = sourceConfig.getMaxCharactersPerFetch();
                        dataNavigatorData.setDataRange(DataRange.fromByteOffset(byteOffset, maxChars));
                    });
                });
            }
        } else {
            progressPresenter.setVisible(false);
        }
    }

    private void updateEditor() {
        if (lastResult.hasErrors()) {
            showErrors(lastResult);
        } else {
            textPresenter.setText(lastResult.getData());
            int firstLineNo = receivedSourceLocation.getOptDataRange()
                    .flatMap(DataRange::getOptLocationFrom)
                    .map(Location::getLineNo)
                    .orElse(1);

            textPresenter.setFirstLineNumber(firstLineNo);

            setEditorMode(lastResult);

            updateEditorHighlights();
        }
    }

    private void showErrors(final FetchDataResult result) {
        final String childStreamText = lastResult.getSourceLocation().getOptChildType()
                .map(childType -> " (" + childType + ")")
                .orElse("");
        final String title = "Unable to display source ["
                + lastResult.getSourceLocation().getIdentifierString()
                + "]"
                + childStreamText;

        final String errorText = String.join("\n", lastResult.getErrors());
        textPresenter.setErrorText(title, errorText);
    }

    private void updateEditorHighlights() {
        if (currentHighlight != null) {
            final BooleanSupplier isSingleLineData = () -> receivedSourceLocation.getOptDataRange()
                    .flatMap(DataRange::getLineCount)
                    .filter(lineCount -> lineCount == 1)
                    .isPresent();

            final BooleanSupplier isNonZeroCharOffset = () -> receivedSourceLocation.getOptDataRange()
                    .flatMap(DataRange::getOptCharOffsetFrom)
                    .filter(charOffset -> charOffset > 0)
                    .isPresent();

            // This is the highlight range for the editor, not the source data. For single line
            // data they will differ if the editor is not displaying from offset one.
            // It is only an issue for single line data because for multi-line we adjust the editor's
            // starting line no to suit the data.
            TextRange editorHighlight = currentHighlight;

            if (isSingleLineData.getAsBoolean() && isNonZeroCharOffset.getAsBoolean()) {
                final long startOffset = receivedSourceLocation.getDataRange().getCharOffsetFrom();

                if (startOffset != 1) {
                    final int highlightDelta = (int) (currentHighlight.getFrom().getColNo() - startOffset);
                    editorHighlight = currentHighlight.withNewStartPosition(
                            DefaultLocation.of(1, highlightDelta));
                }
            }
            textPresenter.setHighlights(Collections.singletonList(editorHighlight));
        } else {
            textPresenter.setHighlights(null);
        }
    }

    private void updateNavigator(final AbstractFetchDataResult result) {
        if (DataType.SEGMENTED.equals(lastResult.getDataType())) {
            dataNavigatorData.segmentsCount = result.getTotalItemCount();
        } else {
            dataNavigatorData.partsCount = result.getTotalItemCount();
        }

//        DataRange dataRange = Optional.ofNullable(result.getSourceLocation())
//                .map(SourceLocation::getDataRange)
//                .orElse(null);
//
        characterNavigatorPresenter.refreshNavigator();
    }

    private void setTitle(final FetchDataResult fetchDataResult) {
        final String streamType = fetchDataResult.getStreamTypeName();
        final SourceLocation sourceLocation = fetchDataResult.getSourceLocation();
        getView().setTitle(
                fetchDataResult.getFeedName(),
                sourceLocation.getMetaId(),
                sourceLocation.getPartIndex(),
                sourceLocation.getRecordIndex(),
                streamType);
    }

    private void setEditorMode(final FetchDataResult fetchDataResult) {
        final AceEditorMode mode;

        if (fetchDataResult.getSourceLocation() != null
                && StreamTypeNames.META.equals(fetchDataResult.getSourceLocation().getChildType())) {
            mode = AceEditorMode.PROPERTIES;
        } else { // We have no way of knowing what type the data is (could be csv, json, xml) so assume XML
            mode = AceEditorMode.XML;
        }
        textPresenter.setMode(mode);
    }

    @Override
    protected void onBind() {

    }

    private boolean isCurrentDataSegmented() {
        return lastResult != null
                && (DataType.SEGMENTED.equals(lastResult.getDataType())
                || DataType.MARKER.equals(lastResult.getDataType()));
    }

    private boolean isCurrentDataMultiPart() {
        // For now assume segmented and multi-part are mutually exclusive
        return lastResult != null
                && DataType.NON_SEGMENTED.equals(lastResult.getDataType());
    }

    private DataType getCurDataType() {
        return lastResult != null
                ? lastResult.getDataType()
                : null;
    }

    private void beginStepping(ClickEvent clickEvent) {
        beginStepping();
    }

    @Override
    public void clear() {

        // TODO @AT Not sure if I need to implement this
    }

    @Override
    public void beginStepping() {
        BeginPipelineSteppingEvent.fire(
                this,
                null,
                receivedSourceLocation.getOptChildType().orElse(null),
                StepType.REFRESH,
                new StepLocation(
                        receivedSourceLocation.getMetaId(),
                        receivedSourceLocation.getPartIndex(),
                        receivedSourceLocation.getRecordIndex()),
                null);
    }

    public void setClassificationUiHandlers(final ClassificationUiHandlers classificationUiHandlers) {
        this.classificationUiHandlers = classificationUiHandlers;
    }


    // ===================================================================


    private class DataNavigatorData implements HasCharacterData {

        private Count<Long> partsCount = Count.of(0L, false);
        private Count<Long> segmentsCount = Count.of(0L, false);

        @Override
        public boolean areNavigationControlsVisible() {
            return !isSteppingSource;
        }

        @Override
        public DataRange getDataRange() {
            return Optional.ofNullable(lastResult)
                    .map(AbstractFetchDataResult::getSourceLocation)
                    .flatMap(SourceLocation::getOptDataRange)
                    .orElse(DataRange.fromCharOffset(0));
        }

        @Override
        public void setDataRange(final DataRange dataRange) {
//            doWithConfig(sourceConfig -> {
            final SourceLocation newSourceLocation = requestedSourceLocation.copy()
                    .withDataRange(dataRange)
                    .build();

            setSourceLocation(newSourceLocation);
//            });
        }

        @Override
        public boolean isSegmented() {
            return DataType.SEGMENTED.equals(lastResult.getDataType());
        }

        @Override
        public Count<Long> getTotalChars() {

            if (lastResult != null && lastResult.getTotalCharacterCount() != null) {
                return lastResult.getTotalCharacterCount();
            } else {
                return Count.approximately(0L);
            }
        }

        @Override
        public Optional<Long> getTotalBytes() {
            return Optional.ofNullable(lastResult)
                    .flatMap(FetchDataResult::getOptTotalBytes);
        }

        @Override
        public void showHeadCharacters() {
            doWithConfig(sourceConfig ->
                    setDataRange(DataRange.fromCharOffset(
                            0,
                            sourceConfig.getMaxCharactersPerFetch())));
        }

        @Override
        public void advanceCharactersForward() {
            doWithConfig(sourceConfig ->
                    setDataRange(DataRange.fromCharOffset(
                            receivedSourceLocation.getDataRange().getCharOffsetTo() + 1,
                            sourceConfig.getMaxCharactersPerFetch())));
        }

        @Override
        public void advanceCharactersBackwards() {
            doWithConfig(sourceConfig -> {
                final long maxChars = sourceConfig.getMaxCharactersPerFetch();
                setDataRange(DataRange.fromCharOffset(
                        receivedSourceLocation.getDataRange().getCharOffsetFrom() - maxChars,
                        maxChars));
            });
        }

        @Override
        public void refresh() {
            setSourceLocation(requestedSourceLocation, true);
        }
    }


    // ===================================================================


    public interface SourceView extends View {

        void setProgressView(final ProgressView progressView);

        void setTextView(final TextView textView);

        void setNavigatorView(final CharacterNavigatorView characterNavigatorView);

        ButtonView addButton(final Preset preset);

        void setTitle(final String feedName,
                      final long id,
                      final long partNo,
                      final long segmentNo,
                      final String type);
    }
}
