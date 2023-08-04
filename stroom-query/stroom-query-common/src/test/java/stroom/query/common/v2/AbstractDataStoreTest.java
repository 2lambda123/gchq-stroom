/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.query.common.v2;

import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValLong;
import stroom.dashboard.expression.v1.ValString;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.Format;
import stroom.query.api.v2.OffsetRange;
import stroom.query.api.v2.ParamSubstituteUtil;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.ResultRequest;
import stroom.query.api.v2.Row;
import stroom.query.api.v2.SearchRequestSource;
import stroom.query.api.v2.Sort;
import stroom.query.api.v2.Sort.SortDirection;
import stroom.query.api.v2.TableResult;
import stroom.query.api.v2.TableSettings;
import stroom.query.common.v2.format.FieldFormatter;
import stroom.query.common.v2.format.FormatterFactory;
import stroom.util.logging.Metrics;
import stroom.util.shared.ModelStringUtil;

import org.junit.jupiter.api.BeforeAll;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractDataStoreTest {

    @BeforeAll
    static void beforeAll() {
        Metrics.setEnabled(true);
    }

    void basicTest() {
        final FormatterFactory formatterFactory = new FormatterFactory(null);
        final FieldFormatter fieldFormatter = new FieldFormatter(formatterFactory);

        final TableSettings tableSettings = TableSettings.builder()
                .addFields(Field.builder()
                        .id("Text")
                        .name("Text")
                        .expression(ParamSubstituteUtil.makeParam("Text"))
                        .format(Format.TEXT)
                        .build())
                .build();

        final DataStore dataStore = create(tableSettings);

        for (int i = 0; i < 3000; i++) {
            final String text = "Text " + i;
            dataStore.add(Val.of(ValString.create(text)));
        }

        // Wait for all items to be added.
        try {
            dataStore.getCompletionState().signalComplete();
            dataStore.getCompletionState().awaitCompletion();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        // Make sure we only get 50 results.
        final ResultRequest tableResultRequest = ResultRequest.builder()
                .componentId("componentX")
                .addMappings(tableSettings)
                .requestedRange(new OffsetRange(0, 50))
                .build();
        final TableResultCreator tableComponentResultCreator = new TableResultCreator(
                fieldFormatter);
        final TableResult searchResult = (TableResult) tableComponentResultCreator.create(
                dataStore,
                tableResultRequest);
        assertThat(searchResult.getResultRange().getLength()).isEqualTo(50);
    }

    void noValuesTest() {
        final FormatterFactory formatterFactory = new FormatterFactory(null);
        final FieldFormatter fieldFormatter = new FieldFormatter(formatterFactory);

        final TableSettings tableSettings = TableSettings.builder()
                .addFields(Field.builder()
                        .id("currentUser")
                        .name("currentUser")
                        .expression("currentUser()")
                        .format(Format.TEXT)
                        .build())
                .build();

        final DataStore dataStore = create(tableSettings);

        for (int i = 0; i < 1; i++) {
            dataStore.add(Val.of(ValString.create("jbloggs")));
        }

        // Wait for all items to be added.
        try {
            dataStore.getCompletionState().signalComplete();
            dataStore.getCompletionState().awaitCompletion();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        // Make sure we only get 50 results.
        final ResultRequest tableResultRequest = ResultRequest.builder()
                .componentId("componentX")
                .addMappings(tableSettings)
                .requestedRange(new OffsetRange(0, 1))
                .build();
        final TableResultCreator tableComponentResultCreator = new TableResultCreator(
                fieldFormatter);
        final TableResult searchResult = (TableResult) tableComponentResultCreator.create(
                dataStore,
                tableResultRequest);
        assertThat(searchResult.getTotalResults().intValue()).isEqualTo(1);
    }

    void testBigBigResult() {
        for (int i = 0; i < 20; i++) {
            System.out.println("\n------ RUN " + (i + 1) + " -------");
            final long start = System.currentTimeMillis();
            testBigResult();
            System.out.println("Took " + ModelStringUtil.formatDurationString(System.currentTimeMillis() - start));
        }
    }

    void testBigResult() {
        final FormatterFactory formatterFactory = new FormatterFactory(null);
        final FieldFormatter fieldFormatter = new FieldFormatter(formatterFactory);

        final TableSettings tableSettings = TableSettings.builder()
                .addFields(Field.builder()
                        .id("Text")
                        .name("Text")
                        .expression(ParamSubstituteUtil.makeParam("Text"))
                        .format(Format.TEXT)
                        .group(0)
                        .build())
                .addFields(Field.builder()
                        .id("Text2")
                        .name("Text2")
                        .expression(ParamSubstituteUtil.makeParam("Text2"))
                        .format(Format.TEXT)
                        .build())
                .showDetail(true)
                .build();

        final DataStoreSettings dataStoreSettings = DataStoreSettings
                .createBasicSearchResultStoreSettings()
                .copy()
                .maxResults(Sizes.create(Integer.MAX_VALUE))
                .storeSize(Sizes.create(Integer.MAX_VALUE)).build();
        final DataStore dataStore = create(tableSettings, dataStoreSettings);

        Metrics.measure("Loaded data", () -> {
            for (int i = 0; i < 100; i++) {
                final String key = UUID.randomUUID().toString();
                for (int j = 0; j < 100000; j++) {
                    final String value = UUID.randomUUID().toString();
                    dataStore.add(Val.of(ValString.create(key), ValString.create(value)));
                }
            }
        });

        // Wait for all items to be added.
        try {
            dataStore.getCompletionState().signalComplete();
            dataStore.getCompletionState().awaitCompletion();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        System.out.println("\nLoading data");
        Metrics.report();

        System.out.println("\nGetting data");
        Metrics.report();

        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();

        runtime.gc();

        //Print used memory
        System.out.println("Used Memory: "
                + ModelStringUtil.formatIECByteSizeString(runtime.totalMemory() - runtime.freeMemory()));

        Metrics.measure("Result", () -> {
            // Make sure we only get 50 results.
            final ResultRequest tableResultRequest = ResultRequest.builder()
                    .componentId("componentX")
                    .addMappings(tableSettings)
                    .requestedRange(new OffsetRange(0, 50))
                    .build();
            final TableResultCreator tableComponentResultCreator = new TableResultCreator(
                    fieldFormatter);
            final TableResult searchResult = (TableResult) tableComponentResultCreator.create(
                    dataStore,
                    tableResultRequest);

            assertThat(searchResult.getTotalResults().intValue()).isEqualTo(50);
        });

        System.out.println("\nGetting results");
        Metrics.report();

    }

    void sortedTextTest() {
        final Sort sort = new Sort(0, SortDirection.ASCENDING);

        final TableSettings tableSettings = TableSettings.builder()
                .addFields(Field.builder()
                        .id("Text")
                        .name("Text")
                        .expression(ParamSubstituteUtil.makeParam("Text"))
                        .sort(sort)
                        .build())
                .build();

        final DataStore dataStore = create(tableSettings);

        for (int i = 0; i < 3000; i++) {
            final String text = "Text " + (int) (Math.random() * 100);
            dataStore.add(Val.of(ValString.create(text)));
        }

        // Wait for all items to be added.
        try {
            dataStore.getCompletionState().signalComplete();
            dataStore.getCompletionState().awaitCompletion();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        final ResultRequest tableResultRequest = ResultRequest.builder()
                .componentId("componentX")
                .addMappings(tableSettings)
                .requestedRange(new OffsetRange(0, 50))
                .build();
        checkResults(dataStore, tableResultRequest, 0, false);
    }

    void sortedNumberTest() {
        final Sort sort = new Sort(0, SortDirection.ASCENDING);

        final TableSettings tableSettings = TableSettings.builder()
                .addFields(Field.builder()
                        .id("Number")
                        .name("Number")
                        .expression(ParamSubstituteUtil.makeParam("Number"))
                        .sort(sort)
                        .build())
                .build();

        final DataStore dataStore = create(tableSettings);

        for (int i = 0; i < 3000; i++) {
            final String text = String.valueOf((int) (Math.random() * 100));
            dataStore.add(Val.of(ValString.create(text)));
        }

        // Wait for all items to be added.
        try {
            dataStore.getCompletionState().signalComplete();
            dataStore.getCompletionState().awaitCompletion();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        final ResultRequest tableResultRequest =
                ResultRequest.builder()
                        .componentId("componentX")
                        .addMappings(tableSettings)
                        .requestedRange(new OffsetRange(0, 50))
                        .build();
        checkResults(dataStore, tableResultRequest, 0, true);
    }

    void sortedCountedTextTest1() {
        final Sort sort = new Sort(0, SortDirection.ASCENDING);

        final TableSettings tableSettings = TableSettings.builder()
                .addFields(Field.builder()
                        .id("Count")
                        .name("Count")
                        .expression("count()")
                        .sort(sort)
                        .build())
                .addFields(Field.builder()
                        .id("Text")
                        .name("Text")
                        .expression(ParamSubstituteUtil.makeParam("Text"))
                        .group(0)
                        .build())
                .build();

        final DataStore dataStore = create(tableSettings);

        for (int i = 0; i < 3000; i++) {
            final String text = "Text " + (int) (Math.random() * 100);
            dataStore.add(Val.of(ValString.create(text)));
        }

        // Wait for all items to be added.
        try {
            dataStore.getCompletionState().signalComplete();
            dataStore.getCompletionState().awaitCompletion();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        final ResultRequest tableResultRequest =
                ResultRequest.builder()
                        .componentId("componentX")
                        .addMappings(tableSettings)
                        .requestedRange(new OffsetRange(0, 50))
                        .build();
        checkResults(dataStore, tableResultRequest, 0, true);
    }

    void sortedCountedTextTest2() {
        final Sort sort = new Sort(0, SortDirection.ASCENDING);

        final TableSettings tableSettings = TableSettings.builder()
                .addFields(Field.builder()
                        .id("Count")
                        .name("Count")
                        .expression("count()")
                        .build())
                .addFields(Field.builder()
                        .id("Text")
                        .name("Text")
                        .expression(ParamSubstituteUtil.makeParam("Text"))
                        .sort(sort)
                        .group(0)
                        .build())
                .build();

        final DataStore dataStore = create(tableSettings);

        for (int i = 0; i < 3000; i++) {
            final String text = "Text " + (int) (Math.random() * 100);
            dataStore.add(Val.of(ValString.create(text)));
        }

        // Wait for all items to be added.
        try {
            dataStore.getCompletionState().signalComplete();
            dataStore.getCompletionState().awaitCompletion();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        final ResultRequest tableResultRequest =
                ResultRequest.builder()
                        .componentId("componentX")
                        .addMappings(tableSettings)
                        .requestedRange(new OffsetRange(0, 50))
                        .build();
        checkResults(dataStore, tableResultRequest, 1, false);
    }

    void sortedCountedTextTest3() {
        final Sort sort = new Sort(0, SortDirection.ASCENDING);

        final TableSettings tableSettings = TableSettings.builder()
                .addFields(Field.builder()
                        .id("Count")
                        .name("Count")
                        .expression("count()")
                        .build())
                .addFields(Field.builder()
                        .id("Text")
                        .name("Text")
                        .expression(ParamSubstituteUtil.makeParam("Text"))
                        .sort(sort)
                        .group(0)
                        .build())
                .build();

        final DataStore dataStore = create(tableSettings);

        for (int i = 0; i < 3000; i++) {
            final String text = "Text " + (int) (Math.random() * 100);
            dataStore.add(Val.of(ValString.create(text)));
        }

        // Wait for all items to be added.
        try {
            dataStore.getCompletionState().signalComplete();
            dataStore.getCompletionState().awaitCompletion();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        final ResultRequest tableResultRequest =
                ResultRequest.builder()
                        .componentId("componentX")
                        .addMappings(tableSettings)
                        .requestedRange(new OffsetRange(0, 50))
                        .build();
        checkResults(dataStore, tableResultRequest, 1, false);
    }

    void firstLastSelectorTest() {
        final Sort sort = new Sort(0, SortDirection.ASCENDING);

        final String param = ParamSubstituteUtil.makeParam("Number");
        final TableSettings tableSettings = TableSettings.builder()
                .addFields(Field.builder()
                        .id("Group")
                        .name("Group")
                        .expression("${group}")
                        .group(0)
                        .build())
                .addFields(Field.builder()
                        .id("Number")
                        .name("Number")
                        .expression("concat(first(" + param + "), ' ', last(" + param + "))")
                        .build())
                .addFields(Field.builder()
                        .id("Number Sorted")
                        .name("Number Sorted")
                        .expression(param)
                        .sort(sort)
                        .build())
                .build();

        final DataStore dataStore = create(tableSettings);

        for (int i = 1; i <= 30; i++) {
            dataStore.add(Val.of(ValString.create("group"), ValLong.create(i)));
        }

        // Wait for all items to be added.
        try {
            dataStore.getCompletionState().signalComplete();
            dataStore.getCompletionState().awaitCompletion();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        final ResultRequest tableResultRequest =
                ResultRequest.builder()
                        .componentId("componentX")
                        .addMappings(tableSettings)
                        .requestedRange(new OffsetRange(0, 50))
                        .build();

        final FormatterFactory formatterFactory = new FormatterFactory(null);
        final FieldFormatter fieldFormatter = new FieldFormatter(formatterFactory);

        final TableResultCreator tableComponentResultCreator = new TableResultCreator(
                fieldFormatter);
        final TableResult searchResult = (TableResult) tableComponentResultCreator.create(dataStore,
                tableResultRequest);

        assertThat(searchResult.getTotalResults()).isOne();

        for (final Row result : searchResult.getRows()) {
            final String value = result.getValues().get(1);
            System.out.println(value);
            assertThat(value).isEqualTo("1 30");
        }
    }

    private void checkResults(final DataStore dataStore,
                              final ResultRequest tableResultRequest,
                              final int sortCol,
                              final boolean numeric) {
        final FormatterFactory formatterFactory = new FormatterFactory(null);
        final FieldFormatter fieldFormatter = new FieldFormatter(formatterFactory);

        // Make sure we only get 2000 results.
        final TableResultCreator tableComponentResultCreator = new TableResultCreator(
                fieldFormatter);
        final TableResult searchResult = (TableResult) tableComponentResultCreator.create(dataStore,
                tableResultRequest);

        assertThat(searchResult.getResultRange().getLength() <= 50).isTrue();
        assertThat(searchResult.getTotalResults() <= 3000).isTrue();

        String lastValue = null;
        for (final Row result : searchResult.getRows()) {
            final String value = result.getValues().get(sortCol);
            if (lastValue != null) {
                if (numeric) {
                    final double d1 = Double.parseDouble(lastValue);
                    final double d2 = Double.parseDouble(value);

                    final int diff = Double.compare(d1, d2);

                    assertThat(diff <= 0).isTrue();
                } else {
                    final int diff = lastValue.compareTo(value);
                    assertThat(diff <= 0).isTrue();
                }
            }
            lastValue = value;
        }
    }

    DataStore create(final TableSettings tableSettings) {
        // Create a set of sizes that are the minimum values for the combination of user provided sizes for the table
        // and the default maximum sizes.
        final Sizes defaultMaxResultsSizes = Sizes.create(50);
        final Sizes storeSize = Sizes.create(100);
        final Sizes maxResults = Sizes.min(Sizes.create(tableSettings.getMaxResults()), defaultMaxResultsSizes);
        final DataStoreSettings dataStoreSettings = DataStoreSettings
                .createBasicSearchResultStoreSettings()
                .copy()
                .maxResults(maxResults)
                .storeSize(storeSize)
                .build();
        return create(tableSettings, dataStoreSettings);
    }

    DataStore create(final TableSettings tableSettings, final DataStoreSettings dataStoreSettings) {
        return create(
                SearchRequestSource.createBasic(),
                new QueryKey(UUID.randomUUID().toString()),
                "0",
                tableSettings,
                new SearchResultStoreConfig(),
                dataStoreSettings);
    }

    abstract DataStore create(SearchRequestSource searchRequestSource,
                              QueryKey queryKey,
                              String componentId,
                              TableSettings tableSettings,
                              AbstractResultStoreConfig resultStoreConfig,
                              DataStoreSettings dataStoreSettings);
}
