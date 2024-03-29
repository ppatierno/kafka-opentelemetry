package io.ppatierno.kafka.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.KafkaTelemetry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier;

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

    private static class TracingKafkaClientSupplier extends DefaultKafkaClientSupplier {
        @Override
        public Producer<byte[], byte[]> getProducer(Map<String, Object> config) {
            KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
            return telemetry.wrap(super.getProducer(config));
        }

        @Override
        public Consumer<byte[], byte[]> getConsumer(Map<String, Object> config) {
            KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
            return telemetry.wrap(super.getConsumer(config));
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
