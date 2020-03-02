package stroom.statistics.impl.hbase.pipeline;

import stroom.docref.DocRef;
import stroom.kafka.pipeline.AbstractKafkaAppender;
import stroom.kafka.pipeline.KafkaProducerFactory;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.errorhandler.LoggedException;
import stroom.pipeline.factory.ConfigurableElement;
import stroom.pipeline.factory.PipelineProperty;
import stroom.pipeline.factory.PipelinePropertyDocRef;
import stroom.pipeline.shared.ElementIcons;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.statistics.impl.hbase.entity.StroomStatsStoreStore;
import stroom.statistics.impl.hbase.internal.HBaseStatisticsConfig;
import stroom.statistics.impl.hbase.shared.StroomStatsStoreDoc;
import stroom.util.shared.Severity;

import javax.inject.Inject;

/**
 * A Kafka appender specifically for sending statistic event messages to kafka.
 * The key and topic are derived from the selected statistic data source
 */
@SuppressWarnings("unused")
@ConfigurableElement(
        type = "StroomStatsAppender",
        category = PipelineElementType.Category.DESTINATION,
        roles = {
                PipelineElementType.ROLE_TARGET,
                PipelineElementType.ROLE_DESTINATION,
                PipelineElementType.VISABILITY_STEPPING},
        icon = ElementIcons.STROOM_STATS)
class StroomStatsAppender extends AbstractKafkaAppender {
    private final StroomStatsStoreStore stroomStatsStoreStore;
    private final HBaseStatisticsConfig hBaseStatisticsConfig;
    private String topic;
    private String recordKey;
    private DocRef stroomStatStoreRef;

    @SuppressWarnings("unused")
    @Inject
    public StroomStatsAppender(final ErrorReceiverProxy errorReceiverProxy,
                               final stroom.kafkanew.pipeline.KafkaProducerFactory stroomKafkaProducerFactoryService,
                               final HBaseStatisticsConfig hBaseStatisticsConfig,
                               final StroomStatsStoreStore stroomStatsStoreStore) {
        super(errorReceiverProxy, stroomKafkaProducerFactoryService);
        this.hBaseStatisticsConfig = hBaseStatisticsConfig;
        this.stroomStatsStoreStore = stroomStatsStoreStore;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public String getRecordKey() {
        return recordKey;
    }

    @Override
    public void startProcessing() {
        if (stroomStatStoreRef == null) {
            super.log(Severity.FATAL_ERROR, "Stroom-Stats data source has not been set", null);
            throw new LoggedException("Stroom-Stats data source has not been set");
        }

        final StroomStatsStoreDoc stroomStatsStoreEntity = stroomStatsStoreStore.readDocument(stroomStatStoreRef);

        if (stroomStatsStoreEntity == null) {
            super.log(Severity.FATAL_ERROR, "Unable to find Stroom-Stats data source " + stroomStatStoreRef, null);
            throw new LoggedException("Unable to find Stroom-Stats data source " + stroomStatStoreRef);
        }

        if (!stroomStatsStoreEntity.isEnabled()) {
            final String msg = "Stroom-Stats data source with name [" + stroomStatsStoreEntity.getName() + "] is disabled";
            log(Severity.FATAL_ERROR, msg, null);
            throw new LoggedException(msg);
        }

        switch (stroomStatsStoreEntity.getStatisticType()) {
            case COUNT:
                topic = hBaseStatisticsConfig.getKafkaTopicsConfig().getCount();
                break;
            case VALUE:
                topic = hBaseStatisticsConfig.getKafkaTopicsConfig().getValue();
                break;
        }
        recordKey = stroomStatsStoreEntity.getUuid();

        super.startProcessing();
    }

    @PipelineProperty(
            description = "The stroom-stats data source to record statistics against.",
            displayPriority = 1)
    @PipelinePropertyDocRef(types = StroomStatsStoreDoc.DOCUMENT_TYPE)
    public void setStatisticsDataSource(final DocRef stroomStatStoreRef) {
        this.stroomStatStoreRef = stroomStatStoreRef;
    }
}
