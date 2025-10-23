package org.codeus.consistent_hashing.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeus.consistent_hashing.consistent_hashing.ConsistentHashing;
import org.codeus.consistent_hashing.dto.*;
import org.codeus.consistent_hashing.util.ProductLoader;
import org.codeus.consistent_hashing.util.StatisticsCollector;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsistentHashingManager {

    private final ProductLoader productLoader;
    private final StatisticsCollector statisticsCollector;
    private ConsistentHashing<Product> consistentHashing;

    public InitResponse initializeHashing(int nodeCount) {
        log.info("Initializing with {} nodes", nodeCount);

        try {
            List<Product> products = productLoader.loadFromJson();

            consistentHashing = new ConsistentHashing<>(Product::getId);
            consistentHashing.setNodeCount(nodeCount);
            consistentHashing.addElements(products);

            return InitResponse.builder()
                    .message("Initialized with " + nodeCount + " nodes")
                    .nodeCount(nodeCount)
                    .totalProducts(consistentHashing.getTotalElements())
                    .checkedElements(consistentHashing.getCheckedElements())
                    .distribution(statisticsCollector.getDistribution(consistentHashing))
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load products", e);
        }
    }

    public AddNodeResponse addNode() {
        validateInitialized();
        log.info("Adding new node");

        Map<String, String> oldDistribution = consistentHashing.captureDistribution();
        consistentHashing.addNode();

        List<NodeMovementSummary> movements = statisticsCollector.trackNodeMovements(consistentHashing, oldDistribution);
        int moved = statisticsCollector.countMovedElements(movements);

        log.info("Node added. Checked: {}, Moved: {}", consistentHashing.getCheckedElements(), moved);

        return AddNodeResponse.builder()
                .message("Node added successfully")
                .nodeCount(consistentHashing.getNodeCount())
                .checkedElements(consistentHashing.getCheckedElements())
                .movedElements(moved)
                .distribution(statisticsCollector.getDistribution(consistentHashing))
                .movements(movements)
                .build();
    }

    public RemoveNodeResponse removeNode(int nodeIndex) {
        validateInitialized();
        log.info("Removing node at index {}", nodeIndex);

        Map<String, String> oldDistribution = consistentHashing.captureDistribution();
        consistentHashing.removeNodeByIndex(nodeIndex);

        List<NodeMovementSummary> movements = statisticsCollector.trackNodeMovements(consistentHashing, oldDistribution);
        int moved = statisticsCollector.countMovedElements(movements);

        log.info("Node removed. Checked: {}, Moved: {}", consistentHashing.getCheckedElements(), moved);

        return RemoveNodeResponse.builder()
                .message("Node removed successfully")
                .nodeCount(consistentHashing.getNodeCount())
                .checkedElements(consistentHashing.getCheckedElements())
                .movedElements(moved)
                .distribution(statisticsCollector.getDistribution(consistentHashing))
                .movements(movements)
                .build();
    }

    private void validateInitialized() {
        if (consistentHashing == null) {
            throw new IllegalStateException("Not initialized. Call /api/init first");
        }
    }
}