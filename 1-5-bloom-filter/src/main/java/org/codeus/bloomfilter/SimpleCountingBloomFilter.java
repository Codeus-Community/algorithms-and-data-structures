package org.codeus.bloomfilter;

/**
 * A simple Counting Bloom Filter implementation with configurable number of hash functions.
 *
 * @param <T> the type of elements to be stored in the filter
 */
public class SimpleCountingBloomFilter<T> {

    //TODO: declare a variable "counters" - an array of int.
    //TODO: Declare a variable "size" of type int;
    //TODO: Declare a variable "numHashes" of type int;

    //TODO: Implement constructor according to javadoc.
    /**
     * Constructs a CountingBloomFilter with the specified size and number of hash functions.
     *
     * @param size      the number of counters (filter size)
     * @param numHashes the number of hash functions to use
     * @throws IllegalArgumentException if size or numHashes is not positive
     */

    //TODO: implement method according to javadoc
    /**
     * Adds the specified item to the filter.
     *
     * @param item the item to add; may be {@code null}
     */
    public void add(T item) {
        throw new UnsupportedOperationException();
    }

    //TODO: implement method according to javadoc
    /**
     * Checks whether the specified item might be present in the filter.
     *
     * @param item the item to check; may be {@code null}
     * @return {@code true} if the item might be present, {@code false} if definitely not present
     */
    public boolean mightContain(T item) {
        throw new UnsupportedOperationException();
    }

    //TODO: implement method according to javadoc
    /**
     * Removes the specified item from the filter.
     * Note: Due to hash collisions, this may cause false negatives.
     *
     * @param item the item to remove; may be {@code null}
     */
    public void remove(T item) {
        throw new UnsupportedOperationException();
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
