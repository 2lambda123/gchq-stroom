package stroom.pipeline.stepping.client.presenter;

import stroom.pipeline.stepping.client.presenter.ElementPresenter.IndicatorType;
import stroom.pipeline.stepping.client.presenter.ElementPresenter.LogPaneEntry;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.DefaultLocation;
import stroom.util.shared.Severity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

class TestElementPresenter {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(TestElementPresenter.class);

    @Test
    void test() {
        final List<LogPaneEntry> logPaneEntries = List.of(
                new LogPaneEntry(
                        null,
                        Severity.FATAL_ERROR,
                        null,
                        null,
                        "msg1"),
                new LogPaneEntry(
                        IndicatorType.CODE,
                        Severity.FATAL_ERROR,
                        null,
                        null,
                        "msg2"),
                new LogPaneEntry(
                        IndicatorType.CODE,
                        Severity.FATAL_ERROR,
                        1,
                        DefaultLocation.of(1, 2),
                        "msg3"),
                new LogPaneEntry(
                        IndicatorType.CODE,
                        Severity.FATAL_ERROR,
                        2,
                        DefaultLocation.of(2, 3),
                        "msg4"),
                new LogPaneEntry(
                        IndicatorType.OUTPUT,
                        Severity.FATAL_ERROR,
                        null,
                        null,
                        "msg5"),
                new LogPaneEntry(
                        IndicatorType.OUTPUT,
                        Severity.FATAL_ERROR,
                        2,
                        DefaultLocation.of(2, 2),
                        "msg6"),
                new LogPaneEntry(
                        null,
                        Severity.ERROR,
                        null,
                        null,
                        "msg7"),
                new LogPaneEntry(
                        null,
                        Severity.WARNING,
                        null,
                        null,
                        "msg8"),
                new LogPaneEntry(
                        null,
                        Severity.INFO,
                        null,
                        null,
                        "msg9")
        );

        final List<LogPaneEntry> sortedList = logPaneEntries.stream()
                .sorted()
                .toList();

        LOGGER.info("list:\n{}", sortedList.stream()
                .map(LogPaneEntry::toString)
                .collect(Collectors.joining("\n")));

        Assertions.assertThat(sortedList)
                        .extracting(LogPaneEntry::getMessage)
                                .containsExactly(
                                        "msg1",
                                        "msg2",
                                        "msg3",
                                        "msg4",
                                        "msg5",
                                        "msg6",
                                        "msg7",
                                        "msg8",
                                        "msg9");
    }
}
