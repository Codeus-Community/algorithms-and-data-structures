package org.codeus.jumphash.dashboard;

import org.codeus.jumphash.common.ConsumerInstanceStats;
import org.codeus.jumphash.common.InstanceStatsMessage;
import org.codeus.jumphash.common.ProducerInstanceStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class StatsListener {

    private static final Logger log = LoggerFactory.getLogger(StatsListener.class);

    private final ObjectMapper objectMapper;
    private final DashboardProperties properties;
    private final StatsRepository repository;

    public StatsListener(ObjectMapper objectMapper,
                         DashboardProperties properties,
                         StatsRepository repository) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.repository = repository;
    }

    @KafkaListener(
            topics = "${app.stats-topic}",
            groupId = "dashboard-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onStats(@Payload String payload) {
        try {
            InstanceStatsMessage message = objectMapper.readValue(payload, InstanceStatsMessage.class);
            String componentType = message.componentType() == null
                    ? "consumer"
                    : message.componentType().toLowerCase();

            if ("producer".equals(componentType)) {
                ProducerInstanceStats stats = new ProducerInstanceStats(
                        message.trackedKeys(),
                        message.keysMigrated(),
                        message.assignmentsObserved(),
                        message.partitionCount(),
                        message.partitionChanges(),
                        message.lastSeen()
                );
                repository.updateProducer(message.instanceId(), stats);
            } else {
                ConsumerInstanceStats stats = new ConsumerInstanceStats(
                        message.processed(),
                        message.lag(),
                        message.lastSeen(),
                        message.droppedOnRebalance()
                );
                repository.updateConsumer(message.instanceId(), stats);
            }
        } catch (Exception e) {
            log.warn("Failed to parse stats payload [{}]", payload, e);
        }
    }
}
