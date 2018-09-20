package stroom.pipeline.server.writer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import stroom.connectors.kafka.StroomKafkaProducer;
import stroom.connectors.kafka.StroomKafkaProducerFactoryService;
import stroom.pipeline.destination.RollingDestination;
import stroom.pipeline.destination.RollingKafkaDestination;
import stroom.pipeline.server.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.server.errorhandler.ProcessException;
import stroom.pipeline.server.factory.ConfigurableElement;
import stroom.pipeline.server.factory.PipelineProperty;
import stroom.pipeline.shared.ElementIcons;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.util.spring.StroomScope;

import javax.inject.Inject;
import java.io.IOException;

@Component
@Scope(StroomScope.PROTOTYPE)
@ConfigurableElement(
        type = "RollingKafkaAppender",
        category = PipelineElementType.Category.DESTINATION,
        roles = {
                PipelineElementType.ROLE_TARGET,
                PipelineElementType.ROLE_DESTINATION,
                PipelineElementType.VISABILITY_STEPPING},
        icon = ElementIcons.KAFKA)
public class RollingKafkaAppender extends AbstractRollingAppender {
    private final StroomKafkaProducerFactoryService stroomKafkaProducerFactoryService;
    private final PathCreator pathCreator;
    private final ErrorReceiverProxy errorReceiverProxy;

    private String topic;
    private String recordKey;
    private boolean flushOnSend = true;

    private String key;

    @Inject
    public RollingKafkaAppender(final StroomKafkaProducerFactoryService stroomKafkaProducerFactoryService,
                                final PathCreator pathCreator,
                                final ErrorReceiverProxy errorReceiverProxy) {
        this.stroomKafkaProducerFactoryService = stroomKafkaProducerFactoryService;
        this.pathCreator = pathCreator;
        this.errorReceiverProxy = errorReceiverProxy;
    }

    @Override
    void validateSpecificSettings() {
        if (recordKey == null || recordKey.length() == 0) {
            throw new ProcessException("No recordKey has been specified");
        }

        if (topic == null || topic.length() == 0) {
            throw new ProcessException("No topic has been specified");
        }
    }

    @Override
    Object getKey() throws IOException {
        if (key == null) {
            //this allows us to have two destinations for the same key and topic but with different
            //flush semantics
            key = String.format("%s:%s:%s", this.topic, this.recordKey, Boolean.toString(flushOnSend));
        }
        return key;
    }

    @Override
    public RollingDestination createDestination() throws IOException {
        StroomKafkaProducer stroomKafkaProducer = stroomKafkaProducerFactoryService.getConnector()
                .orElseThrow(() -> new ProcessException("No kafka producer available to use"));

        return new RollingKafkaDestination(
                key,
                getFrequency(),
                getRollSize(),
                System.currentTimeMillis(),
                stroomKafkaProducer,
                recordKey,
                topic,
                flushOnSend);
    }

    @PipelineProperty(description = "The record key to apply to records, used to select partition. Replacement variables can be used in path strings such as ${feed}.")
    public void setRecordKey(final String recordKey) {
        this.recordKey = pathCreator.replaceAll(recordKey);
    }

    @PipelineProperty(description = "The topic to send the record to. Replacement variables can be used in path strings such as ${feed}.")
    public void setTopic(final String topic) {
        this.topic = pathCreator.replaceAll(topic);
    }

    @PipelineProperty(
            description = "Wait for acknowledgement from the Kafka broker when the appender is rolled" +
                    "This is slower but catches errors in the pipeline process",
            defaultValue = "false")
    public void setFlushOnSend(final boolean flushOnSend) {
        this.flushOnSend = flushOnSend;
    }

    @PipelineProperty(description = "Choose the maximum size that a stream can be before it is rolled.",
            defaultValue = "100M")
    public void setRollSize(final String rollSize) {
        super.setRollSize(rollSize);
    }
}
