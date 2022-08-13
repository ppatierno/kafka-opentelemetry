package io.ppatierno.kafka.opentelemetry;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Streams extends BaseStreams {

    // java -javaagent:path/to/opentelemetry-javaagent.jar -Dotel.service.name=my-kafka-service -Dotel.traces.exporter=jaeger -jar target/kafka-streams-agent-1.0-SNAPSHOT-jar-with-dependencies.jar
    public static void main(String[] args) throws IOException {
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
            streams.run(props, null);
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
    }
}
