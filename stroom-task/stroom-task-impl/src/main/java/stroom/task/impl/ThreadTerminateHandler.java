package stroom.task.impl;

import stroom.task.api.TerminateHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class ThreadTerminateHandler implements TerminateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadTerminateHandler.class);

    private final Thread thread;

    public ThreadTerminateHandler(final Thread thread) {
        this.thread = thread;
    }

    @Override
    public void onTerminate() {
        System.out.println("TERMINATING " + thread.getId() + "\n" + Arrays.stream(thread.getStackTrace())
                .map(Objects::toString)
                .filter(str -> str.contains("stroom"))
                .collect(Collectors.joining("\n")));
        thread.interrupt();
    }

    @Override
    public void onDestroy() {
        // Make sure we don't continue to interrupt a thread after the task context is out of scope.
        if (thread.isInterrupted()) {
            LOGGER.debug("Clearing interrupted state");
            final Thread ct = Thread.currentThread();
            if (thread != ct) {
                LOGGER.error("Unexpected current thread");
            }

            if (Thread.interrupted()) {
                if (thread.isInterrupted()) {
                    LOGGER.error("Unable to clear interrupted state");
                } else {
                    LOGGER.debug("Cleared interrupted state");
                }
            }
        }
    }
}
