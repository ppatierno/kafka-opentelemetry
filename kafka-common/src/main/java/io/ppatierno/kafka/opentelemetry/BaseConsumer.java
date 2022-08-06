package io.ppatierno.kafka.opentelemetry;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class BaseConsumer {

    private static final String BOOTSTRAP_SERVERS_ENV_VAR = "BOOTSTRAP_SERVERS";
    private static final String CONSUMER_GROUP_ENV_VAR = "CONSUMER_GROUP";
    private static final String TOPIC_ENV_VAR = "TOPIC";

    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_TOPIC = "my-topic";
    private static final String DEFAULT_CONSUMER_GROUP = "my-consumer-group";

    private static final Logger log = LogManager.getLogger(BaseConsumer.class);

    protected String bootstrapServers;
    protected String consumerGroup;
    protected String topic;
    protected Consumer<String, String> consumer;

    protected AtomicBoolean running = new AtomicBoolean(true);

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

    public void loadConfiguration(Map<String, String> map) {
        this.bootstrapServers = map.getOrDefault(BOOTSTRAP_SERVERS_ENV_VAR, DEFAULT_BOOTSTRAP_SERVERS);
        this.consumerGroup = map.getOrDefault(CONSUMER_GROUP_ENV_VAR, DEFAULT_CONSUMER_GROUP);
        this.topic = map.getOrDefault(TOPIC_ENV_VAR, DEFAULT_TOPIC);
    }

    public Properties loadKafkaConsumerProperties() {
        Properties props = new Properties();
        props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(CommonClientConfigs.GROUP_ID_CONFIG, this.consumerGroup);
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    public void createKafkaConsumer(Properties props) {
        this.consumer = new KafkaConsumer<>(props);
    }
}
