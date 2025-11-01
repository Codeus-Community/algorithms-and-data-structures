package org.codeus.jumphash.common;

/**
 * Statistics emitted by each consumer instance.
 *
 * @param processed            number of events processed since the instance started
 * @param lag                  approximate aggregated lag for partitions owned by this instance
 * @param lastSeen             epoch milli when the instance reported metrics
 * @param droppedOnRebalance   number of partitions dropped during unsafe demo runs
 */
public record ConsumerInstanceStats(long processed, long lag, long lastSeen, long droppedOnRebalance) {

    public static ConsumerInstanceStats empty() {
        long now = System.currentTimeMillis();
        return new ConsumerInstanceStats(0L, 0L, now, 0L);
    }
}
