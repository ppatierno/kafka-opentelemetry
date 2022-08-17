package io.ppatierno.kafka.opentelemetry;

import java.util.Properties;

public class Producer extends BaseProducer {

    // java -javaagent:path/to/opentelemetry-javaagent.jar -Dotel.service.name=my-kafka-service -Dotel.traces.exporter=jaeger -Dotel.metrics.exporter=none -jar kafka-producer-agent/target/kafka-producer-agent-1.0-SNAPSHOT-jar-with-dependencies.jar
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
}
