package org.codeus.consistent_hashing.service;

import org.codeus.consistent_hashing.consistent_hashing.ConsistentHashing;
import org.codeus.consistent_hashing.dto.InitResponse;
import org.codeus.consistent_hashing.dto.Product;
import org.codeus.consistent_hashing.util.ProductLoader;
import org.codeus.consistent_hashing.util.StatisticsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsistentHashingManagerTest {

    @Mock
    private ProductLoader productLoader;

    @Mock
    private StatisticsCollector statisticsCollector;

    private ConsistentHashingManager consistentHashingManager;

    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        consistentHashingManager = new ConsistentHashingManager(productLoader, statisticsCollector);

        testProducts = createTestProducts(1000);
    }

    @Test
    void testInitializeHashing_UsesConsistentHashing() throws Exception {
        // Arrange
        int nodeCount = 5;
        when(productLoader.loadFromJson()).thenReturn(testProducts);
        when(statisticsCollector.getDistribution(any(ConsistentHashing.class))).thenAnswer(invocation -> {
            ConsistentHashing<?> hashing = invocation.getArgument(0);
            return hashing.getDistribution();
        });

        // Act
        InitResponse response = consistentHashingManager.initializeHashing(nodeCount);

        // Assert
        ConsistentHashing<Product> actualHashing = getConsistentHashingField();
        assertNotNull(actualHashing, "ConsistentHashing should be initialized");
        assertEquals(nodeCount, actualHashing.getNodeCount());
        assertEquals(testProducts.size(), actualHashing.getTotalElements());

        assertTrue(actualHashing.getCheckedElements() > 0,
                "Elements should be checked during initialization");

        // Verify mocks were called
        verify(productLoader).loadFromJson();
        verify(statisticsCollector).getDistribution(any(ConsistentHashing.class));
    }

    @Test
    void testConsistentHashingDistribution_NotUniform() throws Exception {
        // Arrange
        int nodeCount = 3;
        when(productLoader.loadFromJson()).thenReturn(testProducts);
        when(statisticsCollector.getDistribution(any(ConsistentHashing.class))).thenAnswer(invocation -> {
            ConsistentHashing<?> hashing = invocation.getArgument(0);
            return hashing.getDistribution();
        });

        // Act
        consistentHashingManager.initializeHashing(nodeCount);

        // Assert
        ConsistentHashing<Product> hashing = getConsistentHashingField();
        Map<String, Integer> distribution = hashing.getDistribution();

        List<Integer> counts = new ArrayList<>(distribution.values());
        Collections.sort(counts);

        int min = counts.get(0);
        int max = counts.get(counts.size() - 1);
        int difference = max - min;

        assertTrue(difference > 10,
                "Distribution should not be perfectly uniform as in modulo hashing. Difference: " + difference);
    }

    @Test
    void testAddNode_MinimalRedistribution() throws Exception {

        // Arrange
        when(productLoader.loadFromJson()).thenReturn(testProducts);
        when(statisticsCollector.getDistribution(any(ConsistentHashing.class))).thenAnswer(invocation -> {
            ConsistentHashing<?> hashing = invocation.getArgument(0);
            return hashing.getDistribution();
        });
        when(statisticsCollector.trackNodeMovements(any(ConsistentHashing.class), any())).thenReturn(new ArrayList<>());
        when(statisticsCollector.countMovedElements(any())).thenReturn(0);

        consistentHashingManager.initializeHashing(4);
        ConsistentHashing<Product> hashing = getConsistentHashingField();

        Map<String, String> distributionBefore = hashing.captureDistribution();

        // Act
        consistentHashingManager.addNode();

        // Assert
        Map<String, String> distributionAfter = hashing.captureDistribution();

        int movedCount = 0;
        for (String key : distributionBefore.keySet()) {
            if (!distributionBefore.get(key).equals(distributionAfter.get(key))) {
                movedCount++;
            }
        }

        int expectedMaxMoved = testProducts.size() / 4 + 100;
        assertTrue(movedCount < expectedMaxMoved,
                String.format("Too many elements moved: %d (expected < %d). This suggests modulo hashing, not consistent hashing",
                        movedCount, expectedMaxMoved));

        assertTrue(hashing.getCheckedElements() < testProducts.size(),
                "Consistent hashing should check only affected elements, not all");
    }

    @Test
    void testRemoveNode_MinimalRedistribution() throws Exception {

        // Arrange
        when(productLoader.loadFromJson()).thenReturn(testProducts);
        when(statisticsCollector.getDistribution(any(ConsistentHashing.class))).thenAnswer(invocation -> {
            ConsistentHashing<?> hashing = invocation.getArgument(0);
            return hashing.getDistribution();
        });
        when(statisticsCollector.trackNodeMovements(any(ConsistentHashing.class), any())).thenReturn(new ArrayList<>());
        when(statisticsCollector.countMovedElements(any())).thenReturn(0);

        consistentHashingManager.initializeHashing(5);
        ConsistentHashing<Product> hashing = getConsistentHashingField();

        Map<String, String> distributionBefore = hashing.captureDistribution();
        Map<String, Integer> nodeDistribution = hashing.getDistribution();

        int nodeToRemove = 0;
        String nodeNameToRemove = "node-" + nodeToRemove;
        int elementsOnRemovedNode = nodeDistribution.get(nodeNameToRemove);

        // Act
        consistentHashingManager.removeNode(nodeToRemove);

        // Assert
        Map<String, String> distributionAfter = hashing.captureDistribution();

        int movedCount = 0;
        for (String key : distributionBefore.keySet()) {
            if (!distributionBefore.get(key).equals(distributionAfter.get(key))) {
                movedCount++;
            }
        }

        assertEquals(elementsOnRemovedNode, movedCount,
                "Only elements from removed node should be moved in consistent hashing");

        assertEquals(elementsOnRemovedNode, hashing.getCheckedElements(),
                "Only elements from removed node should be checked");
    }

    @Test
    void testConsistentHashingRingStructure() throws Exception {

        // Arrange
        int nodeCount = 4;
        when(productLoader.loadFromJson()).thenReturn(testProducts);
        when(statisticsCollector.getDistribution(any(ConsistentHashing.class))).thenAnswer(invocation -> {
            ConsistentHashing<?> hashing = invocation.getArgument(0);
            return hashing.getDistribution();
        });

        consistentHashingManager.initializeHashing(nodeCount);
        ConsistentHashing<Product> hashing = getConsistentHashingField();

        List<Product> sequentialProducts = IntStream.range(0, 100)
                .mapToObj(i -> Product.builder()
                        .id(String.format("prod-%03d", i))
                        .name("Product " + i)
                        .build())
                .collect(Collectors.toList());

        Map<String, String> distribution1 = new HashMap<>();
        for (Product product : sequentialProducts) {
            String nodeName = getNodeForProduct(hashing, product);
            distribution1.put(product.getId(), nodeName);
        }

        List<Product> shuffledProducts = new ArrayList<>(sequentialProducts);
        Collections.shuffle(shuffledProducts);

        Map<String, String> distribution2 = new HashMap<>();
        for (Product product : shuffledProducts) {
            String nodeName = getNodeForProduct(hashing, product);
            distribution2.put(product.getId(), nodeName);
        }

        assertEquals(distribution1, distribution2,
                "Product placement should be deterministic regardless of insertion order");
    }

    private String getNodeForProduct(ConsistentHashing<Product> hashing, Product product) throws Exception {
        Field ringField = ConsistentHashing.class.getDeclaredField("ring");
        ringField.setAccessible(true);
        @SuppressWarnings("unchecked")
        TreeMap<Long, String> ring = (TreeMap<Long, String>) ringField.get(hashing);

        Field md5Field = ConsistentHashing.class.getDeclaredField("md5");
        md5Field.setAccessible(true);
        java.security.MessageDigest md5 = (java.security.MessageDigest) md5Field.get(hashing);

        md5.reset();
        md5.update(product.getId().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] digest = md5.digest();

        long hash = 0;
        for (int i = 0; i < 8; i++) {
            hash = (hash << 8) | (digest[i] & 0xFF);
        }

        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    @Test
    void testInitializeWithZeroNodes() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> consistentHashingManager.initializeHashing(0),
                "Should throw exception when initializing with zero nodes");
    }

    @Test
    void testInitializeWithNegativeNodes() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> consistentHashingManager.initializeHashing(-1),
                "Should throw exception when initializing with negative nodes");
    }

    @Test
    void testRemoveLastNode() throws Exception {
        // Arrange
        when(productLoader.loadFromJson()).thenReturn(testProducts);
        when(statisticsCollector.getDistribution(any(ConsistentHashing.class))).thenAnswer(invocation -> {
            ConsistentHashing<?> hashing = invocation.getArgument(0);
            return hashing.getDistribution();
        });

        consistentHashingManager.initializeHashing(1);

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> consistentHashingManager.removeNode(0),
                "Should throw exception when removing the last node");
    }

    @Test
    void testAddNodeWithoutInitialization() {
        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> consistentHashingManager.addNode(),
                "Should throw exception when adding node without initialization");
    }

    @Test
    void testRemoveNodeWithoutInitialization() {
        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> consistentHashingManager.removeNode(0),
                "Should throw exception when removing node without initialization");
    }

    private List<Product> createTestProducts(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> Product.builder()
                        .id("product-" + i)
                        .name("Product " + i)
                        .build())
                .collect(Collectors.toList());
    }

    private ConsistentHashing<Product> getConsistentHashingField() throws Exception {
        Field field = ConsistentHashingManager.class.getDeclaredField("consistentHashing");
        field.setAccessible(true);
        return (ConsistentHashing<Product>) field.get(consistentHashingManager);
    }

    @Test
    void testOnlyConsistentHashingIsUsed_NoSimpleHashing() throws Exception {

        // Arrange
        when(productLoader.loadFromJson()).thenReturn(testProducts);
        when(statisticsCollector.getDistribution(any(ConsistentHashing.class))).thenAnswer(invocation -> {
            ConsistentHashing<?> hashing = invocation.getArgument(0);
            return hashing.getDistribution();
        });

        // Act
        InitResponse response = consistentHashingManager.initializeHashing(5);

        // Assert
        Field[] fields = ConsistentHashingManager.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().getName().contains("SimpleHashing")) {
                fail("ConsistentHashingManager contains SimpleHashing field: " + field.getName() +
                        "\nThis violates the requirement to use only ConsistentHashing!" +
                        "\nRemove SimpleHashing from ConsistentHashingManager implementation.");
            }
        }

        ConsistentHashing<Product> hashing = getConsistentHashingField();
        assertNotNull(hashing, "ConsistentHashing must be initialized");
    }

    @Test
    void testAddNode_DetailedDiagnostics() throws Exception {

        // Arrange
        when(productLoader.loadFromJson()).thenReturn(testProducts);
        when(statisticsCollector.getDistribution(any(ConsistentHashing.class))).thenAnswer(invocation -> {
            ConsistentHashing<?> hashing = invocation.getArgument(0);
            return hashing.getDistribution();
        });
        when(statisticsCollector.trackNodeMovements(any(ConsistentHashing.class), any())).thenReturn(new ArrayList<>());
        when(statisticsCollector.countMovedElements(any())).thenReturn(0);

        try {
            consistentHashingManager.initializeHashing(4);
        } catch (Exception e) {
            fail("Failed to initialize ConsistentHashingManager. " +
                    "\nError: " + e.getMessage() +
                    "\nPossible causes:" +
                    "\n1. SimpleHashing is being used instead of ConsistentHashing" +
                    "\n2. Check ConsistentHashingManager implementation" +
                    "\n3. Ensure only ConsistentHashing is initialized and used");
        }

        ConsistentHashing<Product> hashing = getConsistentHashingField();
        Map<String, String> distributionBefore = hashing.captureDistribution();

        // Act
        consistentHashingManager.addNode();

        // Assert with detailed diagnostics
        Map<String, String> distributionAfter = hashing.captureDistribution();

        int movedCount = 0;
        List<String> movedElements = new ArrayList<>();
        for (String key : distributionBefore.keySet()) {
            String oldNode = distributionBefore.get(key);
            String newNode = distributionAfter.get(key);
            if (!oldNode.equals(newNode)) {
                movedCount++;
                if (movedElements.size() < 5) {
                    movedElements.add(String.format("%s: %s -> %s", key, oldNode, newNode));
                }
            }
        }

        int expectedMaxMoved = testProducts.size() / 4 + 100;

        String diagnosticMessage = String.format(
                "\n=== DIAGNOSTIC INFO ===" +
                        "\nTotal elements: %d" +
                        "\nElements moved: %d (%.2f%%)" +
                        "\nExpected max moved: %d" +
                        "\nElements checked by algorithm: %d" +
                        "\nSample moved elements: %s" +
                        "\n\nIf too many elements moved (%d > %d):" +
                        "\n- This suggests SimpleHashing (modulo) is used instead of ConsistentHashing" +
                        "\n- In SimpleHashing, adding a node causes ALL elements to redistribute" +
                        "\n- In ConsistentHashing, only ~1/n elements should move" +
                        "\n\nCheck ConsistentHashingManager.initializeHashing() and addNode() methods",
                testProducts.size(),
                movedCount,
                (movedCount * 100.0 / testProducts.size()),
                expectedMaxMoved,
                hashing.getCheckedElements(),
                movedElements,
                movedCount,
                expectedMaxMoved
        );

        assertTrue(movedCount < expectedMaxMoved, diagnosticMessage);
    }

    @Test
    void testRemoveNode_DetailedDiagnostics() throws Exception {
        // Arrange
        when(productLoader.loadFromJson()).thenReturn(testProducts);
        when(statisticsCollector.getDistribution(any(ConsistentHashing.class))).thenAnswer(invocation -> {
            ConsistentHashing<?> hashing = invocation.getArgument(0);
            return hashing.getDistribution();
        });
        when(statisticsCollector.trackNodeMovements(any(ConsistentHashing.class), any())).thenReturn(new ArrayList<>());
        when(statisticsCollector.countMovedElements(any())).thenReturn(0);

        consistentHashingManager.initializeHashing(5);
        ConsistentHashing<Product> hashing = getConsistentHashingField();

        Map<String, String> distributionBefore = hashing.captureDistribution();
        Map<String, Integer> nodeDistribution = hashing.getDistribution();

        int nodeToRemove = 0;
        String nodeNameToRemove = "node-" + nodeToRemove;
        int elementsOnRemovedNode = nodeDistribution.get(nodeNameToRemove);

        // Act
        consistentHashingManager.removeNode(nodeToRemove);

        // Assert with diagnostics
        Map<String, String> distributionAfter = hashing.captureDistribution();

        int movedCount = 0;
        Set<String> sourceNodes = new HashSet<>();
        for (String key : distributionBefore.keySet()) {
            String oldNode = distributionBefore.get(key);
            String newNode = distributionAfter.get(key);
            if (!oldNode.equals(newNode)) {
                movedCount++;
                sourceNodes.add(oldNode);
            }
        }

        String diagnosticMessage = String.format(
                "\n=== REMOVE NODE DIAGNOSTIC ===" +
                        "\nRemoved node: %s with %d elements" +
                        "\nActual moved: %d" +
                        "\nElements checked: %d" +
                        "\nSource nodes that lost elements: %s" +
                        "\n\nExpected behavior:" +
                        "\n- ConsistentHashing: Only elements from removed node move (%d elements)" +
                        "\n- SimpleHashing: ALL elements redistribute" +
                        "\n\nIf moved != elements on removed node:" +
                        "\n- You're likely using SimpleHashing instead of ConsistentHashing" +
                        "\n- Check ConsistentHashingManager.removeNode() implementation",
                nodeNameToRemove,
                elementsOnRemovedNode,
                movedCount,
                hashing.getCheckedElements(),
                sourceNodes,
                elementsOnRemovedNode
        );

        assertEquals(elementsOnRemovedNode, movedCount, diagnosticMessage);
    }
}