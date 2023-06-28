package stroom.query.common.v2;

import stroom.dashboard.expression.v1.ref.ErrorConsumer;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.string.ExceptionStringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ErrorConsumerImpl implements ErrorConsumer {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ErrorConsumerImpl.class);

    private static final int MAX_ERROR_COUNT = 100;

    private final Set<String> errors = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicInteger errorCount = new AtomicInteger();

    public ErrorConsumerImpl() {
        LOGGER.debug("Creating errorConsumer {}", this);
    }

    @Override
    public void add(final Supplier<String> message) {
        if (LOGGER.isTraceEnabled()) {
            final RuntimeException exception = new RuntimeException(message.get());
            LOGGER.trace(exception::getMessage, exception);
        }

        final int count = errorCount.incrementAndGet();
        if (count <= MAX_ERROR_COUNT) {
            errors.add(message.get());
        }
    }

    @Override
    public void add(final Throwable exception) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(exception::getMessage, exception);
        }

        if (!ErrorConsumerUtil.isInterruption(exception)) {
            final int count = errorCount.incrementAndGet();
            if (count <= MAX_ERROR_COUNT) {
                errors.add(ExceptionStringUtil.getMessage(exception));
            }
        }
    }

    @Override
    public List<String> getErrors() {
        if (errors.size() > 0) {
            return new ArrayList<>(errors);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> drain() {
        final List<String> copy = getErrors();
        copy.forEach(errors::remove);
        return copy;
    }

    @Override
    public boolean hasErrors() {
        return errorCount.get() > 0;
    }

    @Override
    public String toString() {
        return "id=" + System.identityHashCode(this)
                + " errorCount=" + errorCount.get();
    }
}
