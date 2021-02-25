package stroom.query.common.v2;

import stroom.dashboard.expression.v1.FieldIndex;
import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionParamUtil;
import stroom.query.api.v2.Param;
import stroom.query.api.v2.ResultRequest;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.TableSettings;
import stroom.util.logging.TempTagCloudDebug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;

public class CoprocessorsFactory {

    private final SizesProvider sizesProvider;
    private final DataStoreFactory dataStoreFactory;

    @Inject
    public CoprocessorsFactory(final SizesProvider sizesProvider,
                               final DataStoreFactory dataStoreFactory) {
        this.sizesProvider = sizesProvider;
        this.dataStoreFactory = dataStoreFactory;
    }

    public List<CoprocessorSettings> createSettings(final SearchRequest searchRequest) {
        // Group common settings.
        final Map<TableSettings, Set<String>> groupMap = new HashMap<>();
        for (final ResultRequest resultRequest : searchRequest.getResultRequests()) {
            if (resultRequest.getMappings() != null && resultRequest.getMappings().size() > 0) {
                final String componentId = resultRequest.getComponentId();
                final TableSettings tableSettings = resultRequest.getMappings().get(0);
                if (tableSettings != null) {
                    Set<String> set = groupMap.computeIfAbsent(tableSettings, k -> new HashSet<>());
                    set.add(componentId);
                }
            }
        }

        final List<CoprocessorSettings> coprocessorSettings = new ArrayList<>(groupMap.size());
        int i = 0;
        for (final Entry<TableSettings, Set<String>> entry : groupMap.entrySet()) {
            final TableSettings tableSettings = entry.getKey();
            final Set<String> componentIds = entry.getValue();
            final String[] componentIdArray = componentIds.stream().sorted().toArray(String[]::new);
            coprocessorSettings.add(new TableCoprocessorSettings(i++, componentIdArray, tableSettings));
        }

        return coprocessorSettings;
    }

    public Coprocessors create(final SearchRequest searchRequest) {
        final List<CoprocessorSettings> coprocessorSettingsList = createSettings(searchRequest);
        return create(searchRequest.getKey().getUuid(), coprocessorSettingsList, searchRequest.getQuery().getParams());
    }

    public Coprocessors create(final String queryKey,
                               final List<CoprocessorSettings> coprocessorSettingsList,
                               final List<Param> params) {
        // Create a field index map.
        final FieldIndex fieldIndex = new FieldIndex();

        // Create a parameter map.
        final Map<String, String> paramMap = ExpressionParamUtil.createParamMap(params);

        // Create error consumer.
        final ErrorConsumer errorConsumer = new ErrorConsumer();

        final Map<Integer, Coprocessor> coprocessorMap = new HashMap<>();
        final Map<String, TableCoprocessor> componentIdCoprocessorMap = new HashMap<>();


        if (coprocessorSettingsList != null) {
            TempTagCloudDebug.write("COPROCESSOR SETTINGS LIST SIZE=" + coprocessorSettingsList.size());

            for (final CoprocessorSettings coprocessorSettings : coprocessorSettingsList) {
                final Coprocessor coprocessor = create(queryKey,
                        coprocessorSettings,
                        fieldIndex,
                        paramMap,
                        errorConsumer);

                if (coprocessor != null) {
                    coprocessorMap.put(coprocessorSettings.getCoprocessorId(), coprocessor);

                    if (coprocessor instanceof TableCoprocessor) {
                        final TableCoprocessor tableCoprocessor = (TableCoprocessor) coprocessor;
                        final TableCoprocessorSettings tableCoprocessorSettings =
                                (TableCoprocessorSettings) coprocessorSettings;
                        for (final String componentId : tableCoprocessorSettings.getComponentIds()) {
                            componentIdCoprocessorMap.put(componentId, tableCoprocessor);
                        }
                    }
                } else {
                    TempTagCloudDebug.write("NULL COPROCESSOR");
                }
            }
        } else {
            TempTagCloudDebug.write("NULL COPROCESSOR SETTINGS LIST");
        }

        // Group coprocessors by extraction pipeline.
        final Map<DocRef, Set<Coprocessor>> extractionPipelineCoprocessorMap = new HashMap<>();
        coprocessorMap.values().forEach(coprocessor -> {
            DocRef extractionPipeline = null;

            if (coprocessor instanceof TableCoprocessor) {
                final TableCoprocessor tableCoprocessor = (TableCoprocessor) coprocessor;
                if (tableCoprocessor.getTableSettings().extractValues()) {
                    extractionPipeline = tableCoprocessor.getTableSettings().getExtractionPipeline();
                }
            }

            extractionPipelineCoprocessorMap.computeIfAbsent(extractionPipeline, k ->
                    new HashSet<>()).add(coprocessor);
        });

        return new Coprocessors(
                Collections.unmodifiableMap(coprocessorMap),
                Collections.unmodifiableMap(componentIdCoprocessorMap),
                Collections.unmodifiableMap(extractionPipelineCoprocessorMap),
                fieldIndex,
                errorConsumer);
    }

    private Coprocessor create(final String queryKey,
                               final CoprocessorSettings settings,
                               final FieldIndex fieldIndex,
                               final Map<String, String> paramMap,
                               final Consumer<Throwable> errorConsumer) {
        if (settings instanceof TableCoprocessorSettings) {
            final TableCoprocessorSettings tableCoprocessorSettings = (TableCoprocessorSettings) settings;
            final TableSettings tableSettings = tableCoprocessorSettings.getTableSettings();
            final DataStore dataStore = create(
                    queryKey,
                    String.valueOf(tableCoprocessorSettings.getCoprocessorId()),
                    tableSettings,
                    fieldIndex,
                    paramMap);
            return new TableCoprocessor(tableSettings, dataStore, errorConsumer);
        } else if (settings instanceof EventCoprocessorSettings) {
            final EventCoprocessorSettings eventCoprocessorSettings = (EventCoprocessorSettings) settings;
            return new EventCoprocessor(eventCoprocessorSettings, fieldIndex, errorConsumer);
        }

        return null;
    }

    private DataStore create(final String queryKey,
                             final String componentId,
                             final TableSettings tableSettings,
                             final FieldIndex fieldIndex,
                             final Map<String, String> paramMap) {
        final Sizes storeSizes = sizesProvider.getStoreSizes();

        // Create a set of sizes that are the minimum values for the combination of user provided sizes for the table
        // and the default maximum sizes.
        final Sizes defaultMaxResultsSizes = sizesProvider.getDefaultMaxResultsSizes();
        final Sizes maxResults = Sizes.min(Sizes.create(tableSettings.getMaxResults()), defaultMaxResultsSizes);

        return dataStoreFactory.create(
                queryKey,
                componentId,
                tableSettings,
                fieldIndex,
                paramMap,
                maxResults,
                storeSizes);
    }
}
