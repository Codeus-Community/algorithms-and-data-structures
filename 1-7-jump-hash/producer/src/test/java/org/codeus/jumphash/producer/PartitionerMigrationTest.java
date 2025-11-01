package org.codeus.jumphash.producer;

import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartitionerMigrationTest {

    private static final String TOPIC = "demo.events";
    private static final int SAMPLE_KEYS = 50_000;
    private static final int OLD_PARTITIONS = 3;
    private static final int NEW_PARTITIONS = 4;

    @Disabled
    @Test
    @DisplayName("CustomPartitioner with modulo hashing migrates many keys")
    void moduloRoutingMigratesManyKeys() {
        CustomPartitioner partitioner = new CustomPartitioner();

        Cluster oldCluster = clusterWithPartitions(OLD_PARTITIONS);
        Cluster newCluster = clusterWithPartitions(NEW_PARTITIONS);

        double migrationRate = migrationRatio(partitioner, oldCluster, newCluster);

        logMigrationRate("baseline modulo hashing", migrationRate);

        assertThat(migrationRate)
                .as("Baseline modulo routing should migrate many keys before Jump Hash is introduced (%.3f)", migrationRate)
                .isGreaterThan(0.60);
    }

    @Test
    @DisplayName("CustomPartitioner with Jump Hash keeps migrations low")
    void jumpHashKeepsMigrationsLow() {
        CustomPartitioner partitioner = new CustomPartitioner();

        Cluster oldCluster = clusterWithPartitions(OLD_PARTITIONS);
        Cluster newCluster = clusterWithPartitions(NEW_PARTITIONS);

        double migrationRate = migrationRatio(partitioner, oldCluster, newCluster);

        logMigrationRate("Jump Hash routing", migrationRate);

        assertThat(migrationRate)
                .as("Jump Consistent Hashing should keep migrations low once CustomPartitioner switches to consistent hashing (%.3f)", migrationRate)
                .isLessThan(0.35);
    }

    private static double migrationRatio(CustomPartitioner partitioner,
                                         Cluster oldCluster,
                                         Cluster newCluster) {
        int migrated = 0;
        for (int i = 0; i < SAMPLE_KEYS; i++) {
            String key = "user-" + i;
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

            int oldPartition = partitioner.partition(TOPIC, key, keyBytes, null, null, oldCluster);
            int newPartition = partitioner.partition(TOPIC, key, keyBytes, null, null, newCluster);

            if (oldPartition != newPartition) {
                migrated++;
            }
        }
        return migrated / (double) SAMPLE_KEYS;
    }

    private static void logMigrationRate(String description, double rate) {
        System.out.printf("CustomPartitioner migration rate [%s, %d->%d partitions]: %.3f%n",
                description, OLD_PARTITIONS, NEW_PARTITIONS, rate);
    }

    private static Cluster clusterWithPartitions(int partitionCount) {
        Node broker = new Node(0, "localhost", 9092);
        List<PartitionInfo> partitions = new ArrayList<>(partitionCount);
        Node[] replicas = new Node[]{broker};
        Node[] empty = new Node[0];
        for (int i = 0; i < partitionCount; i++) {
            partitions.add(new PartitionInfo(TOPIC, i, broker, replicas, replicas, empty));
        }
        return new Cluster("test-cluster", List.of(broker), partitions, Collections.<String>emptySet(), Collections.<String>emptySet(), broker);
    }
}
