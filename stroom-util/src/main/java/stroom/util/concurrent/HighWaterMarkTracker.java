package stroom.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Used for tracking the high water mark of concurrent operations.
 * Threads should call increment before doing some concurrent operation
 * then decrement when they have finished.
 */
@ThreadSafe
public class HighWaterMarkTracker {

    private final AtomicInteger concurrentCount = new AtomicInteger();
    private final AtomicInteger highWaterMark = new AtomicInteger();

    /**
     * Increment the number of concurrent things happening, i.e. call it before doing
     * the concurrent activity.
     */
    public void increment() {
        concurrentCount.accumulateAndGet(0, (currentConcurrentCount, ignored) -> {
            final int newConcurrentCount = currentConcurrentCount + 1;

            // Increase high watermark if we are higher than it
            highWaterMark.accumulateAndGet(newConcurrentCount, (currentMaxCount, ignored2) ->
                    Math.max(newConcurrentCount, currentMaxCount));

            // Increase the count of the number of concurrent things
            return newConcurrentCount;
        });
    }

    /**
     * Decrement the number of concurrent things happening, i.e. call it after completing the
     * concurrent activity
     */
    public void decrement() {
        concurrentCount.decrementAndGet();
    }

    /**
     * Do concurrent work while keeping track of the maximum number of threads performing work
     * at once.
     */
    public <T> T getWithHighWaterMarkTracking(final Supplier<T> work) {
        try {
            increment();
            return work.get();
        } finally {
            decrement();
        }
    }

    /**
     * Do concurrent work while keeping track of the maximum number of threads performing work
     * at once.
     */
    public void doWithHighWaterMarkTracking(final Runnable work) {
        try {
            increment();
            work.run();
        } finally {
            decrement();
        }
    }

    /**
     * @return The highest number of concurrent things seen so far
     */
    public int getHighWaterMark() {
        return highWaterMark.get();
    }

    /**
     * @return Number of concurrent things now.
     */
    public int getCurrentCount() {
        return concurrentCount.get();
    }

    @Override
    public String toString() {
        return "HighWaterMarkTracker{" +
                "concurrentCount=" + concurrentCount +
                ", highWaterMark=" + highWaterMark +
                '}';
    }
}
