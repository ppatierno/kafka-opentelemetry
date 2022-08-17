package io.ppatierno.kafka.opentelemetry;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Consumer extends BaseConsumer {

    // java -javaagent:path/to/opentelemetry-javaagent.jar -Dotel.service.name=my-kafka-service -Dotel.traces.exporter=jaeger -Dotel.metrics.exporter=none -Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=true -jar kafka-consumer-agent/target/kafka-consumer-agent-1.0-SNAPSHOT-jar-with-dependencies.jar
    public static void main(String[] args) throws IOException, InterruptedException {
        Consumer consumer = new Consumer();
        consumer.loadConfiguration(System.getenv());
        Properties props = consumer.loadKafkaConsumerProperties();
        consumer.createKafkaConsumer(props);

        CountDownLatch latch = new CountDownLatch(1);
        Thread consumerThread = new Thread(() -> consumer.run(latch));
        consumerThread.start();

        System.in.read();
        consumer.running.set(false);
        latch.await(10000, TimeUnit.MILLISECONDS);
    }
}
