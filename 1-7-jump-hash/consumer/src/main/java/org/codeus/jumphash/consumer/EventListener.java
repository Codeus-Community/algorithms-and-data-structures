package org.codeus.jumphash.consumer;

import org.codeus.jumphash.common.DemoEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class EventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    private final ObjectMapper objectMapper;
    private final ConsumerProperties properties;
    private final ConsumerMetrics metrics;

    public EventListener(ObjectMapper objectMapper,
                         ConsumerProperties properties,
                         ConsumerMetrics metrics) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
    }

    @KafkaListener(
            topics = "${app.events-topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(List<ConsumerRecord<String, String>> batch,
                          Acknowledgment ack,
                          Consumer<String, String> consumer) {
        if (batch.isEmpty()) {
            return;
        }

        boolean unsafeDemo = properties.isUnsafeDemo();
        if (unsafeDemo) {
            // Unsafe mode commits before processing to emulate at-most-once semantics.
            ack.acknowledge();
        }

        long aggregatedLag = 0L;
        try {
            aggregatedLag = processBatch(batch, consumer);
        } finally {
            if (!unsafeDemo) {
                // Safe path commits after the batch is processed.
                ack.acknowledge();
            }
            metrics.recordBatch(batch.size(), aggregatedLag);
        }
    }

    private long processBatch(List<ConsumerRecord<String, String>> batch,
                              Consumer<String, String> consumer) {
        Map<TopicPartition, Long> lastOffsets = new HashMap<>();
        for (ConsumerRecord<String, String> record : batch) {
            lastOffsets.put(new TopicPartition(record.topic(), record.partition()), record.offset());
            try {
                DemoEvent event = objectMapper.readValue(record.value(), DemoEvent.class);
                handleEvent(event);
            } catch (Exception e) {
                log.warn("Failed to decode event from topic {} partition {} offset {}",
                        record.topic(), record.partition(), record.offset(), e);
            }
        }

        long aggregatedLag = 0L;
        try {
            Set<TopicPartition> partitions = lastOffsets.keySet();
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
            for (TopicPartition partition : partitions) {
                long endOffset = endOffsets.getOrDefault(partition, 0L);
                long lastOffset = lastOffsets.get(partition);
                long partitionLag = Math.max(0L, endOffset - lastOffset - 1);
                aggregatedLag += partitionLag;
            }
        } catch (Exception e) {
            log.debug("Failed to compute lag, defaulting to zero", e);
        }

        return aggregatedLag;
    }

    private void handleEvent(DemoEvent event) {
        if (properties.getProcessingDelayMs() > 0) {
            try {
                Thread.sleep(properties.getProcessingDelayMs());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        // Demo processing step could be extended with metric aggregation etc.
        log.debug("Processed event for user {} type {}", event.userId(), event.eventType());
    }
}
