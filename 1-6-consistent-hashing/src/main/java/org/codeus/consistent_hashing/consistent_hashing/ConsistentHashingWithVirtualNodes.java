package org.codeus.consistent_hashing.consistent_hashing;

import org.codeus.consistent_hashing.exception.NodeNotFoundException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;

public class ConsistentHashingWithVirtualNodes<T> {
    private final TreeMap<Long, String> ring;
    private final Map<String, Map<String, T>> nodeStorage;
    private final Function<T, String> keyExtractor;
    private final List<T> allElements;
    private final MessageDigest md5;
    private int checkedElements;
    private int nodeCounter;
    private final int virtualNodes = 100;

    public ConsistentHashingWithVirtualNodes(Function<T, String> keyExtractor) {
        this.ring = new TreeMap<>();
        this.nodeStorage = new HashMap<>();
        this.keyExtractor = keyExtractor;
        this.allElements = new ArrayList<>();
        this.checkedElements = 0;
        this.nodeCounter = 0;

        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * Set the initial number of physical nodes with virtual nodes
     * @param nodeCount number of physical nodes
     */
    public void setNodeCount(int nodeCount) {
        if (nodeCount <= 0) {
            throw new IllegalArgumentException("Node count must be positive");
        }
        ring.clear();
        nodeStorage.clear();
        nodeCounter = 0;

        for (int i = 0; i < nodeCount; i++) {
            String nodeName = "node-" + nodeCounter;
            nodeCounter++;
            addNodeInternal(nodeName);
        }

        if (!allElements.isEmpty()) {
            redistributeAll();
        }
    }

    public void addNode() {
        String nodeName = "node-" + nodeCounter;
        nodeCounter++;

        // Create storage for the new physical node
        nodeStorage.put(nodeName, new HashMap<>());

        // Add all virtual nodes for this physical node to the ring
        for (int i = 0; i < virtualNodes; i++) {
            String virtualNodeName = nodeName + "#VN" + i;
            long hash = hash(virtualNodeName);
            ring.put(hash, nodeName);
        }

        // Redistribute elements after adding the node
        redistributeAfterAdd();
    }

    public void removeNodeByIndex(int nodeIndex) {
        String nodeName = "node-" + nodeIndex;

        if (!nodeStorage.containsKey(nodeName)) {
            throw new NodeNotFoundException("Node '" + nodeName + "' not found or already removed");
        }

        if (nodeStorage.size() <= 1) {
            throw new IllegalStateException("Cannot remove last node");
        }

        // Remove all virtual nodes for this physical node from the ring
        for (int i = 0; i < virtualNodes; i++) {
            String virtualNodeName = nodeName + "#VN" + i;
            long hash = hash(virtualNodeName);
            ring.remove(hash);
        }

        // Redistribute elements before removing the node storage
        redistributeAfterRemove(nodeName);

        // Remove the physical node storage
        nodeStorage.remove(nodeName);
    }

    public void addElements(List<T> elements) {
        if (nodeStorage.isEmpty()) {
            throw new IllegalStateException("No nodes available. Call setNodeCount() first");
        }

        allElements.addAll(elements);

        checkedElements = 0;

        for (T element : elements) {
            checkedElements++;
            String key = keyExtractor.apply(element);
            String nodeName = getNodeForKey(key);
            nodeStorage.get(nodeName).put(key, element);
        }
    }

    /**
     * Get the distribution of elements across nodes
     * @return Map with element count for each node
     */
    public Map<String, Integer> getDistribution() {
        Map<String, Integer> distribution = new LinkedHashMap<>();

        List<String> sortedNodes = new ArrayList<>(nodeStorage.keySet());
        Collections.sort(sortedNodes);

        for (String nodeName : sortedNodes) {
            distribution.put(nodeName, nodeStorage.get(nodeName).size());
        }

        return distribution;
    }

    /**
     * Capture current distribution for comparison
     */
    public Map<String, String> captureDistribution() {
        Map<String, String> distribution = new HashMap<>();

        for (T element : allElements) {
            String key = keyExtractor.apply(element);
            String nodeName = getNodeForKey(key);
            distribution.put(key, nodeName);
        }

        return distribution;
    }

    public int getNodeCount() {
        return nodeStorage.size();
    }

    public int getTotalElements() {
        return allElements.size();
    }

    public int getCheckedElements() {
        return checkedElements;
    }

    private void addNodeInternal(String nodeName) {
        nodeStorage.put(nodeName, new HashMap<>());

        // Add all virtual nodes for this physical node
        for (int i = 0; i < virtualNodes; i++) {
            String virtualNodeName = nodeName + "#VN" + i;
            long hash = hash(virtualNodeName);
            ring.put(hash, nodeName);
        }
    }

    private String getNodeForKey(String key) {
        if (ring.isEmpty()) {
            return null;
        }

        long hash = hash(key);
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);

        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    /**
     * MD5 hash function
     */
    private long hash(String key) {
        md5.reset();
        md5.update(key.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md5.digest();

        long hash = 0;
        for (int i = 0; i < 8; i++) {
            hash = (hash << 8) | (digest[i] & 0xFF);
        }

        return hash;
    }

    private void redistributeAll() {
        checkedElements = 0;

        for (Map<String, T> storage : nodeStorage.values()) {
            storage.clear();
        }

        for (T element : allElements) {
            checkedElements++;
            String key = keyExtractor.apply(element);
            String nodeName = getNodeForKey(key);
            nodeStorage.get(nodeName).put(key, element);
        }
    }

    /**
     * Redistribute after adding a node with virtual nodes
     * With virtual nodes, we need to check all elements for proper redistribution
     * because virtual nodes are distributed across the entire ring
     */
    private void redistributeAfterAdd() {
        checkedElements = 0;

        // Collect all data from all nodes
        Map<String, T> allData = new HashMap<>();
        for (Map<String, T> storage : nodeStorage.values()) {
            allData.putAll(storage);
            storage.clear();
        }

        // Redistribute all data according to new ring configuration
        for (Map.Entry<String, T> entry : allData.entrySet()) {
            checkedElements++;
            String key = entry.getKey();
            String nodeName = getNodeForKey(key);
            nodeStorage.get(nodeName).put(key, entry.getValue());
        }
    }

    /**
     * Redistribute after removing a node
     * Move ONLY elements from the removed node
     */
    private void redistributeAfterRemove(String removedNodeName) {
        checkedElements = 0;

        Map<String, T> removedStorage = nodeStorage.get(removedNodeName);
        List<T> elementsToMove = new ArrayList<>(removedStorage.values());

        for (T element : elementsToMove) {
            checkedElements++;
            String key = keyExtractor.apply(element);
            String newNodeName = getNodeForKey(key);

            // Don't try to add to the same node being removed
            if (!newNodeName.equals(removedNodeName)) {
                nodeStorage.get(newNodeName).put(key, element);
            }
        }
    }
}