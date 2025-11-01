package org.codeus.jumphash.dashboard;

import org.codeus.jumphash.common.ConsumerInstanceStats;
import org.codeus.jumphash.common.ProducerInstanceStats;
import org.codeus.jumphash.common.StatsSnapshot;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StatsRepository {

    private final ConcurrentHashMap<String, ConsumerInstanceStats> consumers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProducerInstanceStats> producers = new ConcurrentHashMap<>();
    private volatile long updatedAt = 0L;

    public void updateConsumer(String instanceId, ConsumerInstanceStats stats) {
        consumers.put(instanceId, stats);
        updatedAt = System.currentTimeMillis();
    }

    public void updateProducer(String instanceId, ProducerInstanceStats stats) {
        producers.put(instanceId, stats);
        updatedAt = System.currentTimeMillis();
    }

    public StatsSnapshot snapshot() {
        return new StatsSnapshot(
                Map.copyOf(consumers),
                Map.copyOf(producers),
                updatedAt
        );
    }
}
