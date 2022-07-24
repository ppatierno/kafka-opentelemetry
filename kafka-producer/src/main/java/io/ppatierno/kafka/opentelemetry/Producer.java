package io.ppatierno.kafka.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.instrumentation.kafkaclients.TracingProducerInterceptor;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class Producer extends BaseProducer {

    public static void main(String[] args) throws InterruptedException {
        configureOpenTelemetry();

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

    private static void configureOpenTelemetry() {
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesBuilder.put(ResourceAttributes.SERVICE_NAME, "my-kafka-service");
        Attributes attributes = attributesBuilder.build();

        Resource resource = Resource.create(attributes, ResourceAttributes.SCHEMA_URL);

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder().build()).build())
                .setSampler(Sampler.alwaysOn())
                .setResource(resource)
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }
}
