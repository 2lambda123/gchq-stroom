package stroom.core.receive;

import stroom.util.config.annotations.RequiresRestart;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.validation.ValidRegex;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import javax.inject.Singleton;

@Singleton
public class ReceiveDataConfig extends AbstractConfig {
    /**
     * Same size as JDK's Buffered Output Stream.
     */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private String receiptPolicyUuid;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private String unknownClassification = "UNKNOWN CLASSIFICATION";
    private String feedNamePattern = "^[A-Z0-9_-]{3,}$";

    @JsonPropertyDescription("The UUID of the data receipt policy to use")
    public String getReceiptPolicyUuid() {
        return receiptPolicyUuid;
    }

    @SuppressWarnings("unused")
    public void setReceiptPolicyUuid(final String receiptPolicyUuid) {
        this.receiptPolicyUuid = receiptPolicyUuid;
    }

    @RequiresRestart(RequiresRestart.RestartScope.SYSTEM)
    @JsonPropertyDescription("If set the default buffer size to use")
    public int getBufferSize() {
        return bufferSize;
    }

    @SuppressWarnings("unused")
    public void setBufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @JsonPropertyDescription("The classification banner to display for data if one is not defined")
    public String getUnknownClassification() {
        return unknownClassification;
    }

    @SuppressWarnings("unused")
    public void setUnknownClassification(final String unknownClassification) {
        this.unknownClassification = unknownClassification;
    }

    @ValidRegex
    @JsonPropertyDescription("The regex pattern for feed names")
    public String getFeedNamePattern() {
        return feedNamePattern;
    }

    @SuppressWarnings("unused")
    public void setFeedNamePattern(final String feedNamePattern) {
        this.feedNamePattern = feedNamePattern;
    }

    @Override
    public String toString() {
        return "DataFeedConfig{" +
                "receiptPolicyUuid='" + receiptPolicyUuid + '\'' +
                ", bufferSize=" + bufferSize +
                ", unknownClassification='" + unknownClassification + '\'' +
                ", feedNamePattern='" + feedNamePattern + '\'' +
                '}';
    }
}
