package io.ppatierno.kafka.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.KafkaTelemetry;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Streams extends BaseStreams {

    // env vars: OTEL_SERVICE_NAME=my-kafka-service, OTEL_TRACES_EXPORTER=jaeger, OTEL_METRICS_EXPORTER=none
    // OR
    // system properties: otel.service.name=my-kafka-service, otel.traces.exporter=jaeger, otel.metrics.exporter=none
    public static void main(String[] args) {
        Streams streams = new Streams();
        streams.loadConfiguration(System.getenv());
        Properties props = streams.loadKafkaStreamsProperties();
        props.put(StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.stop();
                latch.countDown();
            }
        });

        try {
            streams.run(props, new TracingKafkaClientSupplier());
            latch.await();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static class TracingKafkaClientSupplier implements KafkaClientSupplier {

        @Override
        public Admin getAdmin(Map<String, Object> config) {
            return Admin.create(config);
        }

        @Override
        public Producer<byte[], byte[]> getProducer(Map<String, Object> config) {
            KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
            return telemetry.wrap(new KafkaProducer<>(config, new ByteArraySerializer(), new ByteArraySerializer()));
        }

        @Override
        public Consumer<byte[], byte[]> getConsumer(Map<String, Object> config) {
            KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
            return telemetry.wrap(new KafkaConsumer<>(config, new ByteArrayDeserializer(), new ByteArrayDeserializer()));
        }

        @Override
        public Consumer<byte[], byte[]> getRestoreConsumer(Map<String, Object> config) {
            return this.getConsumer(config);
        }

        @Override
        public Consumer<byte[], byte[]> getGlobalConsumer(Map<String, Object> config) {
            return this.getConsumer(config);
        }
    }
}
