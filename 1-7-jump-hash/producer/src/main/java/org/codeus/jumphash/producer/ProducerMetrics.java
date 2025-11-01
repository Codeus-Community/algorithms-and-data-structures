package org.codeus.jumphash.producer;

import org.codeus.jumphash.common.ProducerInstanceStats;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks routing stability metrics for the producer so the dashboard can surface key migrations.
 */
@Component
public class ProducerMetrics {

    private final ConcurrentMap<String, Integer> keyAssignments = new ConcurrentHashMap<>();
    private final AtomicLong keysMigrated = new AtomicLong();
    private final AtomicLong assignmentsObserved = new AtomicLong();
    private final AtomicLong partitionCount = new AtomicLong();
    private final AtomicLong partitionChanges = new AtomicLong();
    private final AtomicLong lastSeen = new AtomicLong(System.currentTimeMillis());

    public void recordAssignment(String key, int partition) {
        assignmentsObserved.incrementAndGet();
        if (key == null) {
            lastSeen.set(System.currentTimeMillis());
            return;
        }
        Integer previous = keyAssignments.put(key, partition);
        if (previous != null && previous != partition) {
            keysMigrated.incrementAndGet();
        }
        lastSeen.set(System.currentTimeMillis());
    }

    public void recordPartitionCount(int currentCount) {
        long previous = partitionCount.getAndSet(currentCount);
        if (previous != 0 && previous != currentCount) {
            partitionChanges.incrementAndGet();
        }
        lastSeen.set(System.currentTimeMillis());
    }

    public ProducerInstanceStats snapshot() {
        long seen = lastSeen.get();
        long timestamp = seen == 0 ? System.currentTimeMillis() : seen;
        return new ProducerInstanceStats(
                keyAssignments.size(),
                keysMigrated.get(),
                assignmentsObserved.get(),
                partitionCount.get(),
                partitionChanges.get(),
                timestamp
        );
    }
}
