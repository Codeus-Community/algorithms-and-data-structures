package org.codeus.consistent_hashing.util;
import org.codeus.consistent_hashing.consistent_hashing.ConsistentHashing;
import org.codeus.consistent_hashing.dto.NodeMovementSummary;
import org.codeus.consistent_hashing.dto.Product;
import org.codeus.consistent_hashing.simple_hashing.SimpleHashing;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StatisticsCollector {

    public Map<String, Integer> getDistribution(ConsistentHashing<Product> hashing) {
        return hashing.getDistribution();
    }

    public Map<String, Integer> getDistribution(SimpleHashing<Product> hashing) {
        return hashing.getDistribution();
    }

    /**
     * Track movements between nodes with count (for ConsistentHashing)
     */
    public List<NodeMovementSummary> trackNodeMovements(ConsistentHashing<Product> hashing,
                                                        Map<String, String> oldDistribution) {
        Map<String, String> currentDistribution = hashing.captureDistribution();
        return calculateMovements(oldDistribution, currentDistribution);
    }

    /**
     * Track movements between nodes with count (for SimpleHashing)
     */
    public List<NodeMovementSummary> trackNodeMovements(SimpleHashing<Product> hashing,
                                                        Map<String, String> oldDistribution) {
        Map<String, String> currentDistribution = hashing.captureDistribution();
        return calculateMovements(oldDistribution, currentDistribution);
    }

    /**
     * Common logic for calculating movements
     */
    private List<NodeMovementSummary> calculateMovements(Map<String, String> oldDistribution,
                                                         Map<String, String> currentDistribution) {
        Map<String, int[]> movementCounts = new HashMap<>();

        for (Map.Entry<String, String> entry : oldDistribution.entrySet()) {
            String productId = entry.getKey();
            String oldNode = entry.getValue();
            String newNode = currentDistribution.get(productId);

            if (!oldNode.equals(newNode)) {
                String key = oldNode + "->" + newNode;
                movementCounts.putIfAbsent(key, new int[]{0});
                movementCounts.get(key)[0]++;
            }
        }

        List<NodeMovementSummary> movements = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : movementCounts.entrySet()) {
            String[] nodes = entry.getKey().split("->");
            movements.add(new NodeMovementSummary(nodes[0], nodes[1], entry.getValue()[0]));
        }

        return movements;
    }

    /**
     * Count total moved elements
     */
    public int countMovedElements(List<NodeMovementSummary> movements) {
        return movements.stream()
                .mapToInt(NodeMovementSummary::getCount)
                .sum();
    }
}