package io.ppatierno.kafka.opentelemetry;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Properties;

public abstract class BaseProducer {

    private static final String BOOTSTRAP_SERVERS_ENV_VAR = "BOOTSTRAP_SERVERS";
    private static final String TOPIC_ENV_VAR = "TOPIC";
    private static final String NUM_MESSAGES_ENV_VAR = "NUM_MESSAGES";
    private static final String DELAY_ENV_VAR = "DELAY";

    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_TOPIC = "my-topic";
    private static final String DEFAULT_NUM_MESSAGES = "1";
    private static final String DEFAULT_DELAY = "0";

    private static final Logger log = LogManager.getLogger(BaseProducer.class);

    protected String bootstrapServers;
    protected String topic;
    protected int numMessages;
    protected long delay;
    protected KafkaProducer<String, String> producer;

    public void run() {
        try {
            for (int i = 0; i < this.numMessages; i++) {
                String message = "my-value-" + i;
                ProducerRecord<String, String> record = new ProducerRecord<>(this.topic, message);
                this.producer.send(record);
                log.info("Message [{}] sent to topic [{}]", message, this.topic);
                Thread.sleep(this.delay);
            }
        } catch (InterruptedException e) {
            // Do nothing
        } finally {
            this.producer.close();
        }
    }

    public void loadConfiguration(Map<String, String> map) {
        this.bootstrapServers = map.getOrDefault(BOOTSTRAP_SERVERS_ENV_VAR, DEFAULT_BOOTSTRAP_SERVERS);
        this.topic = map.getOrDefault(TOPIC_ENV_VAR, DEFAULT_TOPIC);
        this.numMessages = Integer.parseInt(map.getOrDefault(NUM_MESSAGES_ENV_VAR, DEFAULT_NUM_MESSAGES));
        this.delay = Long.parseLong(map.getOrDefault(DELAY_ENV_VAR, DEFAULT_DELAY));
    }

    public Properties loadKafkaProducerProperties() {
        Properties props = new Properties();
        props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return props;
    }

    public void createKafkaProducer(Properties props) {
        this.producer = new KafkaProducer<>(props);
    }
}
