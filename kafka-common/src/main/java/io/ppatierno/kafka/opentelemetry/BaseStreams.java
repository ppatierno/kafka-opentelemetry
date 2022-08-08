package io.ppatierno.kafka.opentelemetry;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Properties;

public class BaseStreams {

    private static final String BOOTSTRAP_SERVERS_ENV_VAR = "BOOTSTRAP_SERVERS";
    private static final String TOPIC_IN_ENV_VAR = "TOPIC_IN";
    private static final String TOPIC_OUT_ENV_VAR = "TOPIC_OUT";
    private static final String APPLICATION_ID_ENV_VAR = "APPLICATION_ID";

    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_TOPIC_IN = "my-topic-in";
    private static final String DEFAULT_TOPIC_OUT = "my-topic-out";
    private static final String DEFAULT_APPLICATION_ID = "my-kafka-streams-app";

    private static final Logger log = LogManager.getLogger(BaseStreams.class);

    protected String bootstrapServers;
    protected String topicIn;
    protected String topicOut;
    protected String applicationId;
    protected KafkaStreams streams;

    public void run(Properties props, KafkaClientSupplier clientSupplier) {
        StreamsBuilder builder = new StreamsBuilder();

        builder.stream(this.topicIn, Consumed.with(Serdes.String(), Serdes.String()))
                .mapValues(s -> s.toUpperCase())
                .to(this.topicOut);

        Topology topology = builder.build();
        log.info(topology.describe());

        if (clientSupplier == null) {
            this.streams = new KafkaStreams(topology, props);
        } else {
            this.streams = new KafkaStreams(topology, props, clientSupplier);
        }
        this.streams.start();
    }

    public void stop() {
        this.streams.close();
    }

    public void loadConfiguration(Map<String, String> map) {
        this.bootstrapServers = map.getOrDefault(BOOTSTRAP_SERVERS_ENV_VAR, DEFAULT_BOOTSTRAP_SERVERS);
        this.topicIn = map.getOrDefault(TOPIC_IN_ENV_VAR, DEFAULT_TOPIC_IN);
        this.topicOut = map.getOrDefault(TOPIC_OUT_ENV_VAR, DEFAULT_TOPIC_OUT);
        this.applicationId = map.getOrDefault(APPLICATION_ID_ENV_VAR, DEFAULT_APPLICATION_ID);
    }

    public Properties loadKafkaStreamsProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, this.applicationId);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return props;
    }
}
