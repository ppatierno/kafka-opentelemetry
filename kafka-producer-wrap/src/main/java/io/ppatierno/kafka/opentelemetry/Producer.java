package io.ppatierno.kafka.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.KafkaTelemetry;

import java.util.Properties;

public class Producer extends BaseProducer {

    // env vars: OTEL_SERVICE_NAME=my-kafka-service, OTEL_TRACES_EXPORTER=jaeger, OTEL_METRICS_EXPORTER=none
    // OR
    // system properties: otel.service.name=my-kafka-service, otel.traces.exporter=jaeger, otel.metrics.exporter=none
    // OR
    // using the configureOpenTelemetry(); from base class to create your own OpenTelemetry instance
    public static void main(String[] args) throws InterruptedException {
        Producer producer = new Producer();
        producer.loadConfiguration(System.getenv());
        Properties props = producer.loadKafkaProducerProperties();
        producer.createKafkaProducer(props);

        Thread producerThread = new Thread(() -> producer.run());
        producerThread.start();

        Thread.sleep(5000);
        producerThread.join();
    }

    @Override
    public void createKafkaProducer(Properties props) {
        super.createKafkaProducer(props);
        KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
        this.producer = telemetry.wrap(this.producer);
    }
}
