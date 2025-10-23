package org.codeus.consistent_hashing.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeus.consistent_hashing.dto.*;
import org.codeus.consistent_hashing.simple_hashing.SimpleHashing;
import org.codeus.consistent_hashing.util.ProductLoader;
import org.codeus.consistent_hashing.util.StatisticsCollector;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleHashingManager {

    private final ProductLoader productLoader;
    private final StatisticsCollector statisticsCollector;
    private SimpleHashing<Product> simpleHashing;

    public InitResponse initializeHashing(int nodeCount) {
        log.info("Initializing Simple Hashing with {} nodes", nodeCount);

        try {
            List<Product> products = productLoader.loadFromJson();

            simpleHashing = new SimpleHashing<>(Product::getId);
            simpleHashing.setNodeCount(nodeCount);
            simpleHashing.addElements(products);

            return InitResponse.builder()
                    .message("Simple Hashing initialized with " + nodeCount + " nodes")
                    .nodeCount(nodeCount)
                    .totalProducts(simpleHashing.getTotalElements())
                    .checkedElements(simpleHashing.getCheckedElements())
                    .distribution(statisticsCollector.getDistribution(simpleHashing))
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load products", e);
        }
    }

    public AddNodeResponse addNode() {
        validateInitialized();
        log.info("Adding new node to Simple Hashing");

        Map<String, String> oldDistribution = simpleHashing.captureDistribution();
        simpleHashing.addNode();

        List<NodeMovementSummary> movements = statisticsCollector.trackNodeMovements(simpleHashing, oldDistribution);
        int moved = statisticsCollector.countMovedElements(movements);

        log.info("Node added. Checked: {}, Moved: {}", simpleHashing.getCheckedElements(), moved);

        return AddNodeResponse.builder()
                .message("Node added successfully")
                .nodeCount(simpleHashing.getNodeCount())
                .checkedElements(simpleHashing.getCheckedElements())
                .movedElements(moved)
                .distribution(statisticsCollector.getDistribution(simpleHashing))
                .movements(movements)
                .build();
    }

    public RemoveNodeResponse removeNode(int nodeIndex) {
        validateInitialized();
        log.info("Removing node at index {} from Simple Hashing", nodeIndex);

        Map<String, String> oldDistribution = simpleHashing.captureDistribution();
        simpleHashing.removeNodeByIndex(nodeIndex);

        List<NodeMovementSummary> movements = statisticsCollector.trackNodeMovements(simpleHashing, oldDistribution);
        int moved = statisticsCollector.countMovedElements(movements);

        log.info("Node removed. Checked: {}, Moved: {}", simpleHashing.getCheckedElements(), moved);

        return RemoveNodeResponse.builder()
                .message("Node removed successfully")
                .nodeCount(simpleHashing.getNodeCount())
                .checkedElements(simpleHashing.getCheckedElements())
                .movedElements(moved)
                .distribution(statisticsCollector.getDistribution(simpleHashing))
                .movements(movements)
                .build();
    }

    private void validateInitialized() {
        if (simpleHashing == null) {
            throw new IllegalStateException("Simple Hashing not initialized. Call /api/simple/init first");
        }
    }
}