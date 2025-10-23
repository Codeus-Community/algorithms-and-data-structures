package org.codeus.consistent_hashing.simple_hashing;


import org.codeus.consistent_hashing.exception.NodeNotFoundException;

import java.util.*;
import java.util.function.Function;

/**
 * Simple Hashing (Modulo Hashing) implementation
 */
public class SimpleHashing<T> {
    private Map<String, Map<String, T>> nodeStorage;
    private Function<T, String> keyExtractor;
    private List<T> allElements;
    private int checkedElements;
    private int nodeCounter;

    public SimpleHashing(Function<T, String> keyExtractor) {
        this.nodeStorage = new LinkedHashMap<>();
        this.keyExtractor = keyExtractor;
        this.allElements = new ArrayList<>();
        this.checkedElements = 0;
        this.nodeCounter = 0;
    }

    /**
     * Set the initial number of physical nodes
     * @param nodeCount number of physical nodes
     */
    public void setNodeCount(int nodeCount) {
        if (nodeCount <= 0) {
            throw new IllegalArgumentException("Node count must be positive");
        }

        nodeStorage.clear();
        nodeCounter = 0;

        for (int i = 0; i < nodeCount; i++) {
            String nodeName = "node-" + nodeCounter;
            nodeCounter++;
            nodeStorage.put(nodeName, new HashMap<>());
        }

        if (!allElements.isEmpty()) {
            redistributeAll();
        }
    }

    public void addNode() {
        String nodeName = "node-" + nodeCounter;
        nodeCounter++;
        nodeStorage.put(nodeName, new HashMap<>());

        redistributeAll();
    }

    public void removeNodeByIndex(int nodeIndex) {
        String nodeName = "node-" + nodeIndex;

        if (!nodeStorage.containsKey(nodeName)) {
            throw new NodeNotFoundException("Node '" + nodeName + "' not found or already removed");
        }

        if (nodeStorage.size() <= 1) {
            throw new IllegalStateException("Cannot remove last node");
        }

        nodeStorage.remove(nodeName);

        redistributeAll();
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

    private String getNodeForKey(String key) {
        if (nodeStorage.isEmpty()) {
            return null;
        }

        int hash = key.hashCode();
        int nodeIndex = Math.abs(hash) % nodeStorage.size();

        List<String> nodeNames = new ArrayList<>(nodeStorage.keySet());
        return nodeNames.get(nodeIndex);
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
}