package org.codeus.jumphash.producer;

import org.codeus.jumphash.common.InstanceStatsMessage;
import org.codeus.jumphash.common.ProducerInstanceStats;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically publishes producer routing metrics to the dashboard topic.
 */
@Component
public class ProducerStatsPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProducerStatsPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ProducerProperties properties;
    private final ProducerMetrics metrics;

    public ProducerStatsPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper,
                                  ProducerProperties properties,
                                  ProducerMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.stats-interval-ms:1000}")
    public void publishSnapshot() {
        refreshPartitionCount();

        ProducerInstanceStats snapshot = metrics.snapshot();
        InstanceStatsMessage message = new InstanceStatsMessage(
                "producer",
                properties.getInstanceId(),
                0L,
                0L,
                snapshot.lastSeen(),
                0L,
                snapshot.trackedKeys(),
                snapshot.keysMigrated(),
                snapshot.assignmentsObserved(),
                snapshot.partitionCount(),
                snapshot.partitionChanges()
        );

        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(properties.getStatsTopic(), message.instanceId(), payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize producer stats payload {}", message, e);
        }
    }

    private void refreshPartitionCount() {
        try {
            List<PartitionInfo> partitions = kafkaTemplate.partitionsFor(properties.getTopic());
            if (partitions != null) {
                metrics.recordPartitionCount(partitions.size());
            }
        } catch (Exception e) {
            log.debug("Failed to refresh partition count for topic {}", properties.getTopic(), e);
        }
    }
}
