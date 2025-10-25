package org.codeus.bloomfilter;

/**
 * A simple, generic Bloom filter implementation for probabilistic set membership testing.
 * <p>
 * A Bloom filter is a space-efficient data structure that allows for fast membership checks
 * with a configurable probability of false positives and no false negatives.
 * This implementation uses a fixed number of hash functions and a boolean array as the underlying bit array.
 * </p>
 * <p>
 * Typical usage:
 * <pre>
 *     SimpleBloomFilter filter = new SimpleBloomFilter(100);
 *     filter.add("apple");
 *     boolean mightContain = filter.mightContain("apple");
 * </pre>
 * </p>
 * <p>
 * <b>Note:</b> False positives are possible (the filter may indicate an item is present when it is not),
 * but false negatives are not (if the filter says an item is not present, it is definitely not present).
 * </p>
 *
 * @param <T> the type of elements to be stored in the Bloom filter
 */
public class SimpleBloomFilter<T> {

    private static final int NUM_HASHES = 3;
    private final boolean[] bits;
    private final int size;

    /**
     * Constructs a new SimpleBloomFilter with the specified bit array size.
     * Initializes size variable with the provided value.
     * Initializes bits array with the provided size.
     *
     * @param size the number of bits in the Bloom filter's internal array; determines the filter's capacity and false positive rate
     * @throws IllegalArgumentException if size is less than or equal to zero
     */
    public SimpleBloomFilter(int size) {
        this.size = size;
        this.bits = new boolean[size];
    }

    /**
     * Adds the specified item to the Bloom filter.
     * <p>
     * The item is hashed {@code NUM_HASHES} times using the filter's hash function,
     * and the corresponding bits in the internal bit array are set to {@code true}.
     * This increases the probability that {@link #mightContain(Object)} will return {@code true} for this item.
     * </p>
     *
     * @param item the item to add to the Bloom filter;
     */
    public void add(T item) {
        for (int i = 0; i < NUM_HASHES; i++) {
            bits[hash(item, i, size)] = true;
        }
    }

    /**
     * Checks whether the specified item might be present in the Bloom filter.
     * <p>
     * The item is hashed {@code NUM_HASHES} times using the filter's hash function,
     * and the corresponding bits in the internal bit array are checked.
     * If all relevant bits are set to {@code true}, this method returns {@code true},
     * indicating the item might be present (with some probability of a false positive).
     * If any bit is {@code false}, the item is definitely not present.
     * </p>
     *
     * @param item the item to check for presence in the Bloom filter;
     * @return {@code true} if the item might be present, {@code false} if the item is definitely not present
     */
    public boolean mightContain(T item) {
        for (int i = 0; i < NUM_HASHES; i++) {
            if (!bits[hash(item, i, size)]) {
                return false;
            }
        }
        return true;
    }

    private int hash(T item, int i, int size) {
        int hashCode = (item == null ? 0 : item.hashCode());
        int combined = hashCode + i * 17;
        return Math.abs(combined) % size;
    }
}