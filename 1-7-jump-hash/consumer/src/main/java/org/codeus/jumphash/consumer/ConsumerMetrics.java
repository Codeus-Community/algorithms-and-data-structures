package org.codeus.jumphash.consumer;

import org.codeus.jumphash.common.ConsumerInstanceStats;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ConsumerMetrics {

    private final AtomicLong processed = new AtomicLong();
    private final AtomicLong lag = new AtomicLong();
    private final AtomicLong lastSeen = new AtomicLong(System.currentTimeMillis());

    public void recordBatch(int processedCount, long aggregatedLag) {
        processed.addAndGet(processedCount);
        lag.set(Math.max(aggregatedLag, 0));
        lastSeen.set(System.currentTimeMillis());
    }

    public ConsumerInstanceStats snapshot(long droppedOnRebalance) {
        return new ConsumerInstanceStats(
                processed.get(),
                lag.get(),
                lastSeen.get(),
                droppedOnRebalance
        );
    }
}
