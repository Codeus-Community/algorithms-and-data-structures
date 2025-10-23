package org.codeus.consistent_hashing.consistent_hashing;
import org.codeus.consistent_hashing.exception.NodeNotFoundException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;

public class ConsistentHashing<T>{
    private final TreeMap<Long, String> ring;
    private final Map<String, Map<String, T>> nodeStorage;
    private final Function<T, String> keyExtractor;
    private final List<T> allElements;
    private final MessageDigest md5;
    private int checkedElements;
    private int nodeCounter;

    public ConsistentHashing(Function<T, String> keyExtractor) {
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
     * Set the initial number of physical nodes
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

        long nodeHash = hash(nodeName);
        ring.put(nodeHash, nodeName);
        nodeStorage.put(nodeName, new HashMap<>());

        redistributeAfterAdd(nodeName, nodeHash);
    }

    public void removeNodeByIndex(int nodeIndex) {
        String nodeName = "node-" + nodeIndex;

        if (!nodeStorage.containsKey(nodeName)) {
            throw new NodeNotFoundException("Node '" + nodeName + "' not found or already removed");
        }

        if (nodeStorage.size() <= 1) {
            throw new IllegalStateException("Cannot remove last node");
        }

        long nodeHash = hash(nodeName);
        ring.remove(nodeHash);

        redistributeAfterRemove(nodeName);

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
        long hash = hash(nodeName);
        ring.put(hash, nodeName);
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
     * Redistribute after adding a node
     * Check ONLY elements from the next node
     */
    private void redistributeAfterAdd(String newNodeName, long newNodeHash) {
        checkedElements = 0;

        Map.Entry<Long, String> nextEntry = ring.higherEntry(newNodeHash);
        if (nextEntry == null) {
            nextEntry = ring.firstEntry();
        }

        String nextNodeName = nextEntry.getValue();

        if (nextNodeName.equals(newNodeName)) {
            return;
        }

        Map<String, T> nextNodeStorage = nodeStorage.get(nextNodeName);
        List<T> elementsToCheck = new ArrayList<>(nextNodeStorage.values());

        List<String> keysToMove = new ArrayList<>();
        for (T element : elementsToCheck) {
            checkedElements++;
            String key = keyExtractor.apply(element);
            String correctNode = getNodeForKey(key);

            if (correctNode.equals(newNodeName)) {
                keysToMove.add(key);
            }
        }

        for (String key : keysToMove) {
            T element = nextNodeStorage.remove(key);
            nodeStorage.get(newNodeName).put(key, element);
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
            nodeStorage.get(newNodeName).put(key, element);
        }
    }
}
