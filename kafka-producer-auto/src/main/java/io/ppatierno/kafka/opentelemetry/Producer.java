package io.ppatierno.kafka.opentelemetry;

import io.opentelemetry.instrumentation.kafkaclients.TracingProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class Producer extends BaseProducer {

    // env vars: OTEL_SERVICE_NAME=my-kafka-service, OTEL_TRACES_EXPORTER=jaeger, OTEL_METRICS_EXPORTER=none
    // OR
    // system properties: otel.service.name=my-kafka-service, otel.traces.exporter=jaeger, otel.metrics.exporter=none
    public static void main(String[] args) throws InterruptedException {
        Producer producer = new Producer();
        producer.loadConfiguration(System.getenv());
        Properties props = producer.loadKafkaProducerProperties();
        props.setProperty(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());
        producer.createKafkaProducer(props);

        Thread producerThread = new Thread(() -> producer.run());
        producerThread.start();

        Thread.sleep(5000);
        producerThread.join();
    }
}
