package io.ppatierno.kafka.opentelemetry;

import io.opentelemetry.instrumentation.kafkaclients.TracingConsumerInterceptor;
import io.opentelemetry.instrumentation.kafkaclients.TracingProducerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.streams.StreamsConfig;

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
        props.put(StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());
        props.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());

        CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.stop();
                latch.countDown();
            }
        });

        try {
            streams.run(props);
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
    }
}
