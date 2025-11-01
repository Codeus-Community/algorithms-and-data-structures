package org.codeus.jumphash.common;

/**
 * Statistics emitted by the producer to highlight routing stability.
 *
 * @param trackedKeys         number of unique keys being tracked
 * @param keysMigrated        number of keys that changed partitions at least once
 * @param assignmentsObserved total partition assignments recorded (including repeats)
 * @param partitionCount      current Kafka partition count observed by the producer
 * @param partitionChanges    number of partition-count changes observed
 * @param lastSeen            epoch milli when the snapshot was generated
 */
public record ProducerInstanceStats(
        long trackedKeys,
        long keysMigrated,
        long assignmentsObserved,
        long partitionCount,
        long partitionChanges,
        long lastSeen
) {
}
