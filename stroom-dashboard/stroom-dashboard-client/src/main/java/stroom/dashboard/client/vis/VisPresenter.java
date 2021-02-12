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
 */

package stroom.dashboard.client.vis;

import stroom.dashboard.client.main.AbstractComponentPresenter;
import stroom.dashboard.client.main.Component;
import stroom.dashboard.client.main.ComponentRegistry.ComponentType;
import stroom.dashboard.client.main.Components;
import stroom.dashboard.client.main.ResultComponent;
import stroom.dashboard.client.main.SearchModel;
import stroom.dashboard.client.query.QueryPresenter;
import stroom.dashboard.client.table.TablePresenter;
import stroom.dashboard.shared.ComponentConfig;
import stroom.dashboard.shared.ComponentResultRequest;
import stroom.dashboard.shared.ComponentSettings;
import stroom.dashboard.shared.TableComponentSettings;
import stroom.dashboard.shared.VisComponentSettings;
import stroom.dashboard.shared.VisResultRequest;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.query.api.v2.Result;
import stroom.query.api.v2.ResultRequest.Fetch;
import stroom.query.api.v2.VisResult;
import stroom.script.client.ScriptCache;
import stroom.script.shared.FetchLinkedScriptRequest;
import stroom.script.shared.ScriptDoc;
import stroom.script.shared.ScriptResource;
import stroom.util.client.JSONUtil;
import stroom.util.shared.EqualsUtil;
import stroom.visualisation.client.presenter.VisFunction;
import stroom.visualisation.client.presenter.VisFunction.LoadStatus;
import stroom.visualisation.client.presenter.VisFunction.StatusHandler;
import stroom.visualisation.client.presenter.VisFunctionCache;
import stroom.visualisation.shared.VisualisationDoc;
import stroom.visualisation.shared.VisualisationResource;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.LayerContainer;
import com.gwtplatform.mvp.client.View;

import java.util.List;

public class VisPresenter extends AbstractComponentPresenter<VisPresenter.VisView>
        implements ResultComponent, StatusHandler {

    private static final ScriptResource SCRIPT_RESOURCE = GWT.create(ScriptResource.class);
    private static final VisualisationResource VISUALISATION_RESOURCE = GWT.create(VisualisationResource.class);

    public static final ComponentType TYPE = new ComponentType(4, "vis", "Visualisation");
    //    private static final int MAX_RESULTS = 1000;
    private static final long UPDATE_INTERVAL = 2000;
    private final VisFunctionCache visFunctionCache;
    private final ScriptCache scriptCache;
    private final RestFactory restFactory;
    private final VisPane visPane;
    private final VisFrame visFrame;

    private VisFunction currentFunction;
    private VisFunction loadedFunction;
    private SearchModel currentSearchModel;
    private JSONObject possibleSettings;
    private JavaScriptObject currentSettings;
    private JavaScriptObject currentData;
    private String currentError;
    private boolean searching;
    private long nextUpdate;
    private Timer updateTimer;
    private JavaScriptObject lastData;
    private double opacity = 0;
    private Fetch fetch;
    private TablePresenter linkedTablePresenter;

    @Inject
    public VisPresenter(final EventBus eventBus, final VisView view,
                        final Provider<VisSettingsPresenter> settingsPresenterProvider,
                        final RestFactory restFactory) {
        super(eventBus, view, settingsPresenterProvider);
        this.visFunctionCache = new VisFunctionCache(eventBus);
        this.scriptCache = new ScriptCache(eventBus);
        this.restFactory = restFactory;

        visFrame = new VisFrame(eventBus);
        visPane = visFrame;
        view.setVisPane(visPane);

        final Style style = visFrame.getElement().getStyle();
        style.setPosition(Position.ABSOLUTE);
        style.setOpacity(0);
        style.setZIndex(2);

        RootPanel.get().add(visFrame);
    }

    @Override
    public double getOpacity() {
        return this.opacity;
    }

    /*****************
     * Start of Layout
     *****************/
    @Override
    public void setOpacity(final double opacity) {
        this.opacity = opacity;
        getWidget().getElement().getStyle().setOpacity(opacity);
        visFrame.getElement().getStyle().setOpacity(opacity);
    }

    @Override
    public void addLayer(final LayerContainer layerContainer) {
        layerContainer.add(getWidget());
        // visFrame.setVisible(true);
    }

    @Override
    public boolean removeLayer() {
        getWidget().removeFromParent();
        // visFrame.setVisible(false);
        return false;
    }

    @Override
    public void onResize() {
        if (getWidget() instanceof RequiresResize) {
            ((RequiresResize) getWidget()).onResize();
        }
        visFrame.onResize();
    }

    /*****************
     * End of Layout
     *****************/

    @Override
    public void onRemove() {
        super.onRemove();
        RootPanel.get().remove(visFrame);
    }

    @Override
    protected void onBind() {
        super.onBind();

        visFunctionCache.bind();
        scriptCache.bind();
        visFrame.bind();
    }

    @Override
    protected void onUnbind() {
        super.onUnbind();
        cleanupSearchModelAssociation();
        if (currentFunction != null) {
            currentFunction.removeStatusHandler(this);
        }
        currentFunction = null;

        visFunctionCache.unbind();
        scriptCache.unbind();
        visFrame.unbind();
    }

    @Override
    public void setComponents(final Components components) {
        super.setComponents(components);
        registerHandler(components.addComponentChangeHandler(event -> {
            if (getVisSettings() != null && EqualsUtil.isEquals(getVisSettings().getTableId(),
                    event.getComponentId())) {
                updateTableId(event.getComponentId());
            }
        }));
    }

    private void updateTableId(final String tableId) {
        final VisComponentSettings.Builder builder = getVisSettings().copy();

        builder.tableId(tableId);

        linkedTablePresenter = null;
        final Component component = getComponents().get(getVisSettings().getTableId());
        if (component instanceof TablePresenter) {
            final TablePresenter tablePresenter = (TablePresenter) component;
            linkedTablePresenter = tablePresenter;

            final TableComponentSettings tableSettings = tablePresenter.getTableSettings();
            builder.tableSettings(tableSettings);
            final String queryId = tableSettings.getQueryId();
            setQueryId(queryId);

        } else {
            builder.tableSettings(null);
            setQueryId(null);
        }

        setSettings(builder.build());
    }

    private void setQueryId(final String queryId) {
        cleanupSearchModelAssociation();

        if (queryId != null) {
            final Component component = getComponents().get(queryId);
            if (component instanceof QueryPresenter) {
                final QueryPresenter queryPresenter = (QueryPresenter) component;
                currentSearchModel = queryPresenter.getSearchModel();
                if (currentSearchModel != null) {
                    currentSearchModel.addComponent(getComponentConfig().getId(), this);
                }
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void startSearch() {
        nextUpdate = 0;
        currentSettings = null;
        currentData = null;
        lastData = null;
        fetch = null;

        if (!searching) {
            searching = true;
            visPane.start();
            updateStatusMessage();
        }
    }

    @Override
    public void endSearch() {
        if (searching) {
            searching = false;
            visPane.end();
            updateStatusMessage();
        }
    }

    @Override
    public void setWantsData(final boolean wantsData) {
        getView().setRefreshing(wantsData);
        if (wantsData) {
            this.fetch = Fetch.CHANGES;
        } else {
            this.fetch = Fetch.NONE;
        }
    }

    private void cleanupSearchModelAssociation() {
        if (currentSearchModel != null) {
            // Remove this component from the list of components the search
            // model expects to update.
            currentSearchModel.removeComponent(getComponentConfig().getId());
            currentSearchModel = null;
        }
    }

    @Override
    public void setData(final Result componentResult) {
        try {
            if (getVisSettings() != null && getVisSettings().getVisualisation() != null) {
                if (componentResult != null) {
                    final VisResult visResult = (VisResult) componentResult;

                    currentSettings = getJSONSettings();
                    currentData = getJSONData(visResult);
                    if (currentError == null) {
                        currentError = visResult.getError();
                    }
                }

                // Put a new function in the cache if there isn't one already.
                final VisFunction visFunction = visFunctionCache.get(getVisSettings().getVisualisation());
                if (visFunction == null) {
                    // Create a new function and put it into the cache.
                    final VisFunction function = visFunctionCache.create(getVisSettings().getVisualisation());

                    // Add a handler to act when the function has been loaded.
                    if (currentFunction != null) {
                        currentFunction.removeStatusHandler(this);
                    }
                    currentFunction = function;
                    function.addStatusHandler(this);

                    // Load the visualisation.
                    loadVisualisation(function, getVisSettings().getVisualisation());

                } else {
                    if (currentFunction != visFunction) {
                        if (currentFunction != null) {
                            currentFunction.removeStatusHandler(this);
                        }
                    }
                    // Ensure the function has this as a handler.
                    currentFunction = visFunction;
                    visFunction.addStatusHandler(this);
                }
            } else if (currentFunction != null) {
                currentFunction.removeStatusHandler(this);
                currentFunction = null;

                getView().showMessage("No visualisation");
            }

            updateStatusMessage();
        } catch (final RuntimeException e) {
            GWT.log(e.getMessage(), e);
        }
    }

    private JavaScriptObject getJSONData(final VisResult visResult) {
        JavaScriptObject data = null;

        // Turn JSON result text into an object.
        final JSONObject dataObject = JSONUtil.getObject(JSONUtil.parse(visResult.getJsonData()));
        if (dataObject != null) {
            data = dataObject.getJavaScriptObject();
        }

        return data;
    }

    private JavaScriptObject getJSONSettings() {
        JavaScriptObject settings = null;

        // Turn JSON settings into an object.
        JSONObject settingsObject = null;
        final VisComponentSettings visDashboardSettings = getVisSettings();
        if (visDashboardSettings != null && visDashboardSettings.getJson() != null) {
            try {
                settingsObject = JSONUtil.getObject(JSONUtil.parse(visDashboardSettings.getJson()));
            } catch (final RuntimeException e) {
                getView().showMessage("Unable to parse settings");
            }
        }

        final JSONObject combinedSettings = combineSettings(possibleSettings, settingsObject);
        if (combinedSettings != null) {
            settings = combinedSettings.getJavaScriptObject();
        }

        return settings;
    }

    private void loadVisualisation(final VisFunction function, final DocRef visualisation) {
        function.setStatus(LoadStatus.LOADING_ENTITY);

        final Rest<VisualisationDoc> rest = restFactory.create();
        rest
                .onSuccess(result -> {
                    if (result != null) {
                        // Get all possible settings for this visualisation.
                        possibleSettings = null;
                        try {
                            if (result.getSettings() != null) {
                                if (result.getSettings() != null) {
                                    possibleSettings = JSONUtil.getObject(JSONUtil.parse(result.getSettings()));
                                }
                            }
                        } catch (final RuntimeException e) {
                            failure(function, "Unable to parse settings for visualisaton: "
                                    + getVisSettings().getVisualisation());
                        }

                        function.setFunctionName(result.getFunctionName());

                        // Do we have required scripts.
                        if (result.getScriptRef() != null) {
                            // Now we have loaded the visualisation, load all
                            // associated scripts.
                            loadScripts(function, result.getScriptRef());

                        } else {
                            // Set the function status to loaded. This will tell all
                            // handlers that the function is ready for use.
                            if (!LoadStatus.FAILURE.equals(function.getStatus())) {
                                function.setStatus(LoadStatus.LOADED);
                            }
                        }
                    } else {
                        failure(function,
                                "No visualisation found for: " + getVisSettings().getVisualisation());
                    }
                })
                .onFailure(caught -> failure(function, caught.getMessage()))
                .call(VISUALISATION_RESOURCE)
                .read(visualisation);
    }

    private void loadScripts(final VisFunction function, final DocRef scriptRef) {
        function.setStatus(LoadStatus.LOADING_SCRIPT);

        final Rest<List<ScriptDoc>> rest = restFactory.create();
        rest
                .onSuccess(result -> startInjectingScripts(result, function))
                .call(SCRIPT_RESOURCE)
                .fetchLinkedScripts(new FetchLinkedScriptRequest(scriptRef, scriptCache.getLoadedScripts()));
    }

    private void startInjectingScripts(final List<ScriptDoc> scripts, final VisFunction function) {
        function.setStatus(LoadStatus.INJECTING_SCRIPT);
        // Inject returned scripts.
        visPane.injectScripts(scripts, function);
    }

    @Override
    public void onChange(final VisFunction function) {
        // Ensure this is a load event for the current function.
        if (function.equals(currentFunction)) {
            if (LoadStatus.LOADED.equals(function.getStatus())) {
                try {
                    if (loadedFunction == null || !loadedFunction.equals(function)) {
                        loadedFunction = function;
                        visPane.setVisType(function.getFunctionName());
                    }

                    if (currentData != null) {
                        update();
                        currentError = null;
                    }

                } catch (final RuntimeException e) {
                    currentError = e.getMessage();
                }
            } else if (LoadStatus.FAILURE.equals(function.getStatus())) {
                // Try and clear the current visualisation.
                try {
                    // getView().clear();
                    currentError = null;
                } catch (final RuntimeException e) {
                    // Ignore.
                }
            }

            updateStatusMessage();
        }
    }

    private void update() {
        final long now = System.currentTimeMillis();
        if (now > nextUpdate) {
            nextUpdate = now + UPDATE_INTERVAL;
            lastData = currentData;
            refreshVisualisation();
        } else {
            if (updateTimer == null) {
                updateTimer = new Timer() {
                    @Override
                    public void run() {
                        if (lastData != currentData) {
                            update();
                        }
                    }
                };
            }
            updateTimer.cancel();
            updateTimer.schedule(1000);
        }
    }

    private void updateStatusMessage() {
        final VisFunction function = currentFunction;
        if (function != null) {
            if (currentError != null) {
                getView().showMessage(currentError);
            } else {
                switch (function.getStatus()) {
                    case NOT_LOADED:
                        break;
                    case LOADING_ENTITY:
                        getView().showMessage("Loading visualisation entity...");
                        break;
                    case LOADING_SCRIPT:
                        getView().showMessage("Loading script...");
                        break;
                    case INJECTING_SCRIPT:
                        getView().showMessage("Injecting script...");
                        break;
                    case LOADED:
                        if (currentError != null) {
                            getView().showMessage(currentError);
                        } else if (currentData == null) {
                            if (searching) {
                                getView().hideMessage();
                                //getView().showMessage("Waiting for data...");
                            } else {
                                getView().showMessage("No data");
                            }
                        } else {
                            getView().hideMessage();
                        }

                        break;
                    case FAILURE:
                        getView().showMessage(function.getStatusMessage());
                }
            }
        }
    }

    private void failure(final VisFunction function, final String message) {
        if (!LoadStatus.FAILURE.equals(function.getStatus())) {
            function.setStatus(LoadStatus.FAILURE, message);
        }
    }

    @Override
    public void read(final ComponentConfig componentConfig) {
        super.read(componentConfig);

        final ComponentSettings settings = componentConfig.getSettings();
        if (!(settings instanceof VisComponentSettings)) {
            setSettings(VisComponentSettings.builder().build());
        }
    }

    private VisComponentSettings getVisSettings() {
        return (VisComponentSettings) getSettings();
    }

    @Override
    public void link() {
        String tableId = getVisSettings().getTableId();
        tableId = getComponents().validateOrGetFirstComponentId(tableId, TablePresenter.TYPE.getId());
        updateTableId(tableId);
    }

    @Override
    protected void changeSettings() {
        super.changeSettings();

        updateTableId(getVisSettings().getTableId());

        // Update the current settings JSON and refresh the visualisation.
        currentSettings = getJSONSettings();
        refreshVisualisation();

        // if (currentSearchModel != null) {
        // currentSearchModel.destroy();
        // }
    }

    private void refreshVisualisation() {
        visPane.setData(getComponents().getContext(), currentSettings, currentData);
    }

    @Override
    public ComponentType getType() {
        return TYPE;
    }

    @Override
    public ComponentResultRequest getResultRequest() {
        // Update table settings.
        updateLinkedTableSettings();
        return VisResultRequest
                .builder()
                .componentId(getId())
                .visDashboardSettings(getVisSettings())
//                .requestedRange(new OffsetRange(0, MAX_RESULTS))
                .fetch(fetch)
                .build();
    }

    @Override
    public ComponentResultRequest createDownloadQueryRequest() {
        // Update table settings.
        updateLinkedTableSettings();
        return VisResultRequest
                .builder()
                .componentId(getId())
                .visDashboardSettings(getVisSettings())
//                .requestedRange(new OffsetRange(0, MAX_RESULTS))
                .fetch(Fetch.ALL)
                .build();
    }

    private void updateLinkedTableSettings() {
        // Update table settings.
        TableComponentSettings tableComponentSettings = null;
        if (linkedTablePresenter != null) {
            tableComponentSettings = linkedTablePresenter.getTableSettings();
        }
        setSettings(getVisSettings()
                .copy()
                .tableSettings(tableComponentSettings)
                .build());
    }

    private JSONObject combineSettings(final JSONObject possibleSettings, final JSONObject dynamicSettings) {
        if (possibleSettings == null) {
            return dynamicSettings;
        }
        if (dynamicSettings == null) {
            return possibleSettings;
        }

        final JSONObject allSettings = new JSONObject();
        final JSONArray tabs = JSONUtil.getArray(possibleSettings.get("tabs"));
        if (tabs != null) {
            for (int i = 0; i < tabs.size(); i++) {
                final JSONObject tab = JSONUtil.getObject(tabs.get(i));
                if (tab != null) {
                    // final String name = JSONUtil.getString(tab.get("name"));
                    final JSONArray controls = JSONUtil.getArray(tab.get("controls"));
                    if (controls != null) {
                        for (int j = 0; j < controls.size(); j++) {
                            final JSONObject control = JSONUtil.getObject(controls.get(j));
                            final String id = JSONUtil.getString(control.get("id"));
                            // final String type =
                            // JSONUtil.getString(control.get("type"));
                            // final String label =
                            // JSONUtil.getString(control.get("label"));

                            final JSONValue val = dynamicSettings.get(id);
                            if (val != null) {
                                allSettings.put(id, val);
                            } else {
                                allSettings.put(id, control.get("defaultValue"));
                            }
                        }
                    }
                }
            }
        }
        return allSettings;
    }

    public interface VisView extends View {

        void setRefreshing(boolean refreshing);

        void showMessage(String message);

        void hideMessage();

        void setVisPane(VisPane visPane);
    }
}
