package org.codeus.jumphash.producer;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mimics Kafka's default murmur2/modulo routing so we can later swap in Jump Hash.
 */
// TODO: switch to Jump Hash by delegating to Hashing.consistentHash.
public class CustomPartitioner implements Partitioner {

    @Override
    public int partition(String topic,
                         Object key,
                         byte[] keyBytes,
                         Object value,
                         byte[] valueBytes,
                         Cluster cluster) {
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int partitionCount = partitions != null ? partitions.size() : 0;
        if (partitionCount <= 0) {
            return 0;
        }

        if (keyBytes == null) {
            // Match DefaultPartitioner behaviour: fall back to a "sticky" random partition when no key is provided.
            return Utils.toPositive(ThreadLocalRandom.current().nextInt()) % partitionCount;
        }

        long hash = Utils.toPositive(Utils.murmur2(keyBytes));
        return (int) (hash % partitionCount);
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
