package org.codeus.jumphash.common;

/**
 * Wire payload sent to the dashboard topic.
 *
 * @param componentType        identifies the producer/consumer component that emitted the stats
 * @param instanceId           unique identifier of the instance
 * @param processed            processed events count (consumers only)
 * @param lag                  aggregated lag for current assignments (consumers only)
 * @param lastSeen             timestamp (epoch milli) when the stats snapshot was taken
 * @param droppedOnRebalance   counter of partitions dropped during unsafe demo runs (consumers only)
 * @param trackedKeys          number of unique keys currently tracked by the producer
 * @param keysMigrated         number of keys that changed the assigned partition since tracking began
 * @param assignmentsObserved  number of partition assignments observed (used to calculate migration ratio)
 * @param partitionCount       most recent partition count reported by the producer
 * @param partitionChanges     number of partition-count changes observed by the producer
 */
public record InstanceStatsMessage(
        String componentType,
        String instanceId,
        long processed,
        long lag,
        long lastSeen,
        long droppedOnRebalance,
        long trackedKeys,
        long keysMigrated,
        long assignmentsObserved,
        long partitionCount,
        long partitionChanges
) {
}
