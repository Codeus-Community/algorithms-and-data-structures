package org.codeus.jumphash.consumer;

import org.codeus.jumphash.common.ConsumerInstanceStats;
import org.codeus.jumphash.common.InstanceStatsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StatsPublisher {

    private static final Logger log = LoggerFactory.getLogger(StatsPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ConsumerProperties properties;
    private final ConsumerMetrics metrics;

    public StatsPublisher(KafkaTemplate<String, String> kafkaTemplate,
                          ObjectMapper objectMapper,
                          ConsumerProperties properties,
                          ConsumerMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.stats-interval-ms:1000}")
    public void publishSnapshot() {
        ConsumerInstanceStats snapshot = metrics.snapshot(KafkaConfig.DROPPED_ON_REBALANCE.get());
        InstanceStatsMessage message = new InstanceStatsMessage(
                "consumer",
                properties.getInstanceId(),
                snapshot.processed(),
                snapshot.lag(),
                snapshot.lastSeen(),
                snapshot.droppedOnRebalance(),
                0L,
                0L,
                0L,
                0L,
                0L
        );
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(properties.getStatsTopic(), message.instanceId(), payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize stats payload {}", message, e);
        }
    }
}
