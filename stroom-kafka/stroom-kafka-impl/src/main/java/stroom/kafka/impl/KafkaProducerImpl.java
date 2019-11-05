/*
 * Copyright 2019 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package stroom.kafka.impl;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.kafka.pipeline.KafkaProducer;
import stroom.kafka.pipeline.KafkaProducerRecord;
import stroom.kafka.pipeline.KafkaRecordMetaData;
import stroom.kafkaConfig.shared.KafkaConfigDoc;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Responsible for sending records to Kafka.
 *
 * Config is defined within KafkaConfigDoc document
 */
class KafkaProducerImpl implements KafkaProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducerImpl.class);

    //instance of a kafka producer that will be shared by all threads
    private final Producer<String, byte[]> producer;

    KafkaProducerImpl(final KafkaConfigDoc kafkaConfigDoc) {
        if (kafkaConfigDoc == null) {
            LOGGER.error("KafkaProducer initialised with null config");
            throw new NullPointerException("Null configuration");
        }

        Properties props = getProperties(kafkaConfigDoc);
        final Object object = props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
        final String bootstrapServers = (object != null) ? object.toString() : null;

        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            final String msg = String.format(
                    "Invalid kafka configuration. Property %s is required.",
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
            LOGGER.error("KafkaProducer initialised with invalid config.  Missing props include ",
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
            throw new RuntimeException(msg);
        }

        LOGGER.info("Initialising kafka producer for {}", bootstrapServers);
        LOGGER.info("Creating Kafka Producer with Props: " + props);

        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            // This bizzarness has to be done to force the Kafka config libraries to use the 'Kafka Classloader'
            // instead of the current thread content class loader.
            // https://issues.apache.org/jira/browse/KAFKA-3218
            Thread.currentThread().setContextClassLoader(null);

            //TODO https://community.hortonworks.com/articles/80813/kafka-best-practices-1.html suggests that
            //having 1 producer per topic yields better performance. We could keep a map of producers keyed
            //on topic, though we would need to kill off producers that were unused to cope with users mistyping
            //topic names.  This could probably only be done once StroomKafkaProducer is separated from the
            //Connector interface

            //Even if kafka is down the producer will still create successfully. Any calls to send will however
            //block until it comes up or throw an exception on timeout
            this.producer = new org.apache.kafka.clients.producer.KafkaProducer(props);
        } catch (final RuntimeException e) {
            throw new RuntimeException(String.format("Error initialising kafka producer for %s, due to %s",
                    bootstrapServers, e.getMessage()), e);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        LOGGER.info("Kafka Producer successfully created (this doesn't guarantee Kafka is available)");
    }


    @Override
    public List<CompletableFuture<KafkaRecordMetaData>> sendAsync(
            final List<KafkaProducerRecord<String, byte[]>> stroomRecords,
            final Consumer<Throwable> exceptionHandler) {

        return send(stroomRecords, exceptionHandler);
    }

    @Override
    public List<KafkaRecordMetaData> sendSync(final List<KafkaProducerRecord<String, byte[]>> stroomRecords) {

        List<CompletableFuture<KafkaRecordMetaData>> futures = send(stroomRecords, null);

        List<KafkaRecordMetaData> metaDataList = new ArrayList<>();
        //Now wait for them all to complete
        futures.forEach(future -> {
            try {
                metaDataList.add(future.get());
            } catch (final InterruptedException e) {
                // Continue to interrupt this thread.
                Thread.currentThread().interrupt();
                LOGGER.warn("Thread {} interrupted", Thread.currentThread().getName());
            } catch (ExecutionException e) {
                //this is sync so throw rather than using the callback
                throw new RuntimeException(
                        String.format("Error sending %s records to kafka", futures.size()), e);
            }
        });
        return metaDataList;
    }

    private List<CompletableFuture<KafkaRecordMetaData>> send(
            final List<KafkaProducerRecord<String, byte[]>> stroomRecords,
            final Consumer<Throwable> exceptionHandler) {

        List<CompletableFuture<KafkaRecordMetaData>> futures = stroomRecords.stream()
                .map(this::mapStroomRecord)
                .map(producerRecord -> {
                    CompletableFuture<KafkaRecordMetaData> future = new CompletableFuture<>();
                    producer.send(producerRecord, (recordMetadata, exception) -> {
                        if (exception != null) {
                            future.completeExceptionally(exception);
                            if (exceptionHandler != null) {
                                exceptionHandler.accept(exception);
                            }
                        } else {
                            future.complete(WrappedRecordMetaData.wrap(recordMetadata));
                        }
                        LOGGER.trace("Record sent to Kafka");
                    });
                    return future;
                })
                .collect(Collectors.toList());

        return futures;
    }

    private ProducerRecord<String, byte[]> mapStroomRecord(final KafkaProducerRecord<String, byte[]> stroomRecord) {
        return new ProducerRecord<>(
                stroomRecord.topic(),
                stroomRecord.partition(),
                stroomRecord.timestamp(),
                stroomRecord.key(),
                stroomRecord.value());
    }

    public void flush() {
        producer.flush();
    }

    @Override
    public void shutdown() {
        try {
            LOGGER.info("Closing down Kafka Producer");
            producer.close();
        } catch (final RuntimeException e) {
            LOGGER.error("Error closing kafka producer", e);
        }
    }

    public static Properties getProperties(KafkaConfigDoc doc){
        Properties properties = new Properties();
        StringReader reader = new StringReader(doc.getData());
        try {
            properties.load(reader);
        }catch (IOException ex){
            LOGGER.error("Unable to read kafka properties", ex);
        }
        return properties;
    }
}
