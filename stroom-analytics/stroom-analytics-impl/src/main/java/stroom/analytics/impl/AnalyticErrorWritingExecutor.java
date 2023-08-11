package stroom.analytics.impl;

import stroom.task.api.TaskContext;
import stroom.task.api.TaskContextFactory;
import stroom.util.pipeline.scope.PipelineScopeRunnable;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Provider;

class AnalyticErrorWritingExecutor {

    private final PipelineScopeRunnable pipelineScopeRunnable;
    private final TaskContextFactory taskContextFactory;
    private final Provider<AnalyticErrorWriter> analyticErrorWriterProvider;

    @Inject
    AnalyticErrorWritingExecutor(final PipelineScopeRunnable pipelineScopeRunnable,
                                 final TaskContextFactory taskContextFactory,
                                 final Provider<AnalyticErrorWriter> analyticErrorWriterProvider) {
        this.pipelineScopeRunnable = pipelineScopeRunnable;
        this.taskContextFactory = taskContextFactory;
        this.analyticErrorWriterProvider = analyticErrorWriterProvider;
    }

    void exec(final String taskName,
              final String errorFeedName,
              final String pipelineUuid,
              final TaskContext parentTaskContext,
              final Consumer<TaskContext> taskContextConsumer) {

        pipelineScopeRunnable.scopeRunnable(() -> {
            taskContextFactory.childContext(parentTaskContext, taskName, taskContext -> {
                final AnalyticErrorWriter analyticErrorWriter = analyticErrorWriterProvider.get();
                analyticErrorWriter.exec(
                        errorFeedName,
                        pipelineUuid,
                        () -> taskContextConsumer.accept(taskContext));
            }).run();
        });
    }
}
