package org.codeus.bloomfilter;

/**
 * A simple Counting Bloom Filter implementation with configurable number of hash functions.
 *
 * @param <T> the type of elements to be stored in the filter
 */
public class SimpleCountingBloomFilter<T> {

    private final int[] counters;
    private final int size;
    private final int numHashes;

    /**
     * Constructs a CountingBloomFilter with the specified size and number of hash functions.
     *
     * @param size      the number of counters (filter size)
     * @param numHashes the number of hash functions to use
     * @throws IllegalArgumentException if size or numHashes is not positive
     */
    public SimpleCountingBloomFilter(int size, int numHashes) {
        if (size <= 0 || numHashes <= 0) {
            throw new IllegalArgumentException("Size and numHashes must be positive");
        }
        this.size = size;
        this.numHashes = numHashes;
        this.counters = new int[size];
    }

    /**
     * Adds the specified item to the filter.
     *
     * @param item the item to add; may be {@code null}
     */
    public void add(T item) {
        for (int i = 0; i < numHashes; i++) {
            counters[hash(item, i, size)]++;
        }
    }

    /**
     * Checks whether the specified item might be present in the filter.
     *
     * @param item the item to check; may be {@code null}
     * @return {@code true} if the item might be present, {@code false} if definitely not present
     */
    public boolean mightContain(T item) {
        for (int i = 0; i < numHashes; i++) {
            if (counters[hash(item, i, size)] == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes the specified item from the filter.
     * Note: Due to hash collisions, this may cause false negatives.
     *
     * @param item the item to remove; may be {@code null}
     */
    public void remove(T item) {
        for (int i = 0; i < numHashes; i++) {
            int idx = hash(item, i, size);
            if (counters[idx] > 0) {
                counters[idx]--;
            }
        }
    }

    /**
     * Simple hash mixing function.
     */
    private int hash(T item, int i, int size) {
        int hashCode = (item == null ? 0 : item.hashCode());
        int combined = hashCode + i * 17;
        return Math.abs(combined) % size;
    }
}
