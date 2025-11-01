package org.codeus.jumphash.producer;

import org.codeus.jumphash.common.DemoEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class EventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EventDispatcher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ProducerProperties properties;
    private final ProducerMetrics metrics;

    public EventDispatcher(KafkaTemplate<String, String> kafkaTemplate,
                           ObjectMapper objectMapper,
                           ProducerProperties properties,
                           ProducerMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.tick-interval-ms:1000}")
    public void emitBatch() {
        int batchSize = Math.max(0, properties.getEventsPerSecond());
        if (batchSize == 0) {
            return;
        }
        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < batchSize; i++) {
            DemoEvent event = nextEvent();
            try {
                String payload = objectMapper.writeValueAsString(event);
                String key = event.userId();
                kafkaTemplate.send(properties.getTopic(), key, payload)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.debug("Failed to publish event {}", key, ex);
                            } else {
                                recordSuccess(key, result);
                            }
                        });
            } catch (JsonProcessingException e) {
                // Log and continue; the scheduler will try again on the next tick.
                log.warn("Failed to serialize event {}", event, e);
            }
        }
        log.debug("Published {} events in {} ms", batchSize, System.currentTimeMillis() - startedAt);
    }

    private DemoEvent nextEvent() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String userId = "user-" + random.nextInt(properties.getUserPoolSize());
        String eventType = properties.getEventTypes()
                .get(random.nextInt(properties.getEventTypes().size()));
        long timestamp = Instant.now().toEpochMilli();
        return new DemoEvent(userId, eventType, timestamp);
    }

    private void recordSuccess(String key, SendResult<String, String> result) {
        if (result == null || result.getRecordMetadata() == null) {
            return;
        }
        metrics.recordAssignment(key, result.getRecordMetadata().partition());
    }
}
