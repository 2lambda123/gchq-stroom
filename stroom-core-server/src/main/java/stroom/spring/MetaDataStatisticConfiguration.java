package stroom.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stroom.internalstatistics.MetaDataStatisticImpl;
import stroom.internalstatistics.MetaDataStatisticTemplate;
import stroom.statistics.internal.InternalStatisticsReceiver;

import java.util.Arrays;

@Configuration
public class MetaDataStatisticConfiguration {
    /**
     * This bean must be returned as a class and not an interface otherwise annotation scanning will not work.
     */
    @Bean
    public MetaDataStatisticImpl metaDataStatistic(final InternalStatisticsReceiver internalStatisticsReceiver) {
        final MetaDataStatisticImpl metaDataStatistic = new MetaDataStatisticImpl(internalStatisticsReceiver);
        metaDataStatistic.setTemplates(Arrays.asList(
                new MetaDataStatisticTemplate(
                        "metaDataStreamSize",
                        "receivedTime",
                        Arrays.asList("Feed")),
                new MetaDataStatisticTemplate(
                        "metaDataStreamsReceived",
                        "receivedTime",
                        "StreamSize",
                        Arrays.asList("Feed"))));
        return metaDataStatistic;
    }

}
