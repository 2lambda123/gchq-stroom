package stroom.data.client.presenter;

public class ProcessChoice {

    private final int priority;
    private final boolean autoPriority;
    private final boolean reprocess;
    private final boolean enabled;
    private final Long minMetaCreateTimeMs;
    private final Long maxMetaCreateTimeMs;

    public ProcessChoice(final int priority,
                         final boolean autoPriority,
                         final boolean reprocess,
                         final boolean enabled,
                         final Long minMetaCreateTimeMs,
                                  final Long maxMetaCreateTimeMs) {
        this.priority = priority;
        this.autoPriority = autoPriority;
        this.reprocess = reprocess;
        this.enabled = enabled;
        this.minMetaCreateTimeMs = minMetaCreateTimeMs;
        this.maxMetaCreateTimeMs = maxMetaCreateTimeMs;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isAutoPriority() {
        return autoPriority;
    }

    public boolean isReprocess() {
        return reprocess;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getMinMetaCreateTimeMs() {
        return minMetaCreateTimeMs;
    }

    public Long getMaxMetaCreateTimeMs() {
        return maxMetaCreateTimeMs;
    }
}
