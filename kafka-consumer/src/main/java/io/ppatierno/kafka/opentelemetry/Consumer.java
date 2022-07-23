package io.ppatierno.kafka.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.instrumentation.kafkaclients.TracingConsumerInterceptor;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Consumer {

    private static final String BOOTSTRAP_SERVERS_ENV_VAR = "BOOTSTRAP_SERVERS";
    private static final String CONSUMER_GROUP_ENV_VAR = "CONSUMER_GROUP";
    private static final String TOPIC_ENV_VAR = "TOPIC";

    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_TOPIC = "my-topic";
    private static final String DEFAULT_CONSUMER_GROUP = "my-consumer-group";

    private static final Logger log = LogManager.getLogger(Consumer.class);

    private String bootstrapServers;
    private String consumerGroup;
    private String topic;
    private KafkaConsumer<String, String> consumer;

    private AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) throws IOException, InterruptedException {
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

        Consumer consumer = new Consumer();
        consumer.loadConfiguration(System.getenv());

        Properties props = new Properties();
        props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, consumer.bootstrapServers);
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(CommonClientConfigs.GROUP_ID_CONFIG, consumer.consumerGroup);
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.setProperty(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());

        CountDownLatch latch = new CountDownLatch(1);

        consumer.createKafkaConsumer(props);

        Thread consumerThread = new Thread(() -> consumer.run(latch));
        consumerThread.start();

        System.in.read();
        consumer.running.set(false);
        latch.await(10000, TimeUnit.MILLISECONDS);
    }

    public void run(CountDownLatch latch) {
        log.info("Subscribe to topic [{}]", this.topic);
        this.consumer.subscribe(List.of(this.topic));
        try {
            log.info("Polling ...");
            while (this.running.get()) {
                ConsumerRecords<String, String> records = this.consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Received message key = [{}], value = [{}], offset = [{}]", record.key(), record.value(), record.offset());
                }
            }
        } catch (WakeupException we) {
            // Ignore exception if closing
            if (running.get()) throw we;
        } finally {
            this.consumer.close();
            latch.countDown();
        }
    }

    private void loadConfiguration(Map<String, String> map) {
        this.bootstrapServers = map.getOrDefault(BOOTSTRAP_SERVERS_ENV_VAR, DEFAULT_BOOTSTRAP_SERVERS);
        this.consumerGroup = map.getOrDefault(CONSUMER_GROUP_ENV_VAR, DEFAULT_CONSUMER_GROUP);
        this.topic = map.getOrDefault(TOPIC_ENV_VAR, DEFAULT_TOPIC);
    }

    public void createKafkaConsumer(Properties props) {
        this.consumer = new KafkaConsumer<>(props);
    }
}
