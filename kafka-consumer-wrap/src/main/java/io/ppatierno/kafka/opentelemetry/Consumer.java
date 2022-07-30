package io.ppatierno.kafka.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.KafkaTelemetry;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Consumer extends BaseConsumer {

    // env vars: OTEL_SERVICE_NAME=my-kafka-service, OTEL_TRACES_EXPORTER=jaeger, OTEL_METRICS_EXPORTER=none
    // OR
    // system properties: otel.service.name=my-kafka-service, otel.traces.exporter=jaeger, otel.metrics.exporter=none
    // OR
    // using the configureOpenTelemetry(); from base class to create your own OpenTelemetry instance
    public static void main(String[] args) throws IOException, InterruptedException {
        Consumer consumer = new Consumer();
        consumer.loadConfiguration(System.getenv());
        Properties props = consumer.loadKafkaProducerProperties();
        consumer.createKafkaConsumer(props);

        CountDownLatch latch = new CountDownLatch(1);
        Thread consumerThread = new Thread(() -> consumer.run(latch));
        consumerThread.start();

        System.in.read();
        consumer.running.set(false);
        latch.await(10000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void createKafkaConsumer(Properties props) {
        super.createKafkaConsumer(props);
        KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
        this.consumer = telemetry.wrap(this.consumer);
    }
}
