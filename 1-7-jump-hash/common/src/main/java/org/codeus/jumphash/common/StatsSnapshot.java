package org.codeus.jumphash.common;

import java.util.Map;

/**
 * Dashboard payload containing per-instance metrics.
 */
public record StatsSnapshot(
        Map<String, ConsumerInstanceStats> consumers,
        Map<String, ProducerInstanceStats> producers,
        long updatedAt
) {
}
