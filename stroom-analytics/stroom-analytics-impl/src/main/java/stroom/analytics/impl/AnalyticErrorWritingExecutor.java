package stroom.analytics.impl;

import stroom.task.api.TaskContext;
import stroom.task.api.TaskContextFactory;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Provider;

class AnalyticErrorWritingExecutor {

    private final TaskContextFactory taskContextFactory;
    private final Provider<AnalyticErrorWriter> analyticErrorWriterProvider;

    @Inject
    AnalyticErrorWritingExecutor(final TaskContextFactory taskContextFactory,
                                 final Provider<AnalyticErrorWriter> analyticErrorWriterProvider) {
        this.taskContextFactory = taskContextFactory;
        this.analyticErrorWriterProvider = analyticErrorWriterProvider;
    }

    Runnable wrap(final String taskName,
                  final String errorFeedName,
                  final String pipelineUuid,
                  final TaskContext parentTaskContext,
                  final Consumer<TaskContext> taskContextConsumer) {
        return taskContextFactory.childContext(parentTaskContext, taskName, taskContext -> {
            final AnalyticErrorWriter analyticErrorWriter = analyticErrorWriterProvider.get();
            analyticErrorWriter.exec(
                    errorFeedName,
                    pipelineUuid,
                    () -> taskContextConsumer.accept(taskContext));
        });
    }
}
