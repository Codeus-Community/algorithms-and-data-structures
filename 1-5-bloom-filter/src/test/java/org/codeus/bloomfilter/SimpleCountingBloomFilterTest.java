package org.codeus.bloomfilter;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleCountingBloomFilterTest {

    @Order(1)
    @Test
    void countersFieldIsPresent() throws NoSuchFieldException {
        Field countersField = SimpleCountingBloomFilter.class.getDeclaredField("counters");
        assertTrue(Modifier.isPrivate(countersField.getModifiers()), "counters should be private");
        assertEquals(int[].class, countersField.getType(), "counters should be of type int[]");
    }

    @Order(2)
    @Test
    void sizeFieldIsPresent() throws NoSuchFieldException {
        Field sizeField = SimpleCountingBloomFilter.class.getDeclaredField("size");
        assertTrue(Modifier.isPrivate(sizeField.getModifiers()), "size should be private");
        assertEquals(int.class, sizeField.getType(), "size should be of type int");
    }

    @Order(3)
    @Test
    void numHashesFieldIsPresent() throws NoSuchFieldException {
        Field numHashesField = SimpleCountingBloomFilter.class.getDeclaredField("numHashes");
        assertTrue(Modifier.isPrivate(numHashesField.getModifiers()), "numHashes should be private");
        assertEquals(int.class, numHashesField.getType(), "numHashes should be of type int");
    }

    @Order(4)
    @Test
    void constructorExistsAndAcceptsSizeAndNumHashes() throws Exception {
        Constructor<?> constructor = SimpleCountingBloomFilter.class.getDeclaredConstructor(int.class, int.class);
        assertNotNull(constructor, "Constructor with (int size, int numHashes) should exist");
    }

    @Order(5)
    @Test
    void onlyOneConstructorExists() {
        Constructor<?>[] constructors = SimpleCountingBloomFilter.class.getDeclaredConstructors();
        assertEquals(1, constructors.length, "There should be exactly one constructor");
    }

    @Order(6)
    @Test
    void fieldsAreInitializedInConstructor() throws Exception {
        Constructor<?> constructor = SimpleCountingBloomFilter.class.getDeclaredConstructor(int.class, int.class);
        Object filter = constructor.newInstance(100, 3);

        Field countersField = SimpleCountingBloomFilter.class.getDeclaredField("counters");
        countersField.setAccessible(true);
        int[] counters = (int[]) countersField.get(filter);
        assertNotNull(counters, "counters should be initialized");
        assertEquals(100, counters.length, "counters array should have length equal to size");

        Field sizeField = SimpleCountingBloomFilter.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        int size = (int) sizeField.get(filter);
        assertEquals(100, size, "size field should be initialized to constructor value");

        Field numHashesField = SimpleCountingBloomFilter.class.getDeclaredField("numHashes");
        numHashesField.setAccessible(true);
        int numHashes = (int) numHashesField.get(filter);
        assertEquals(3, numHashes, "numHashes field should be initialized to constructor value");
    }

    @Order(7)
    @Test
    void addSetsCounters() throws Exception {
        Constructor<?> constructor = SimpleCountingBloomFilter.class.getDeclaredConstructor(int.class, int.class);
        Object filter = constructor.newInstance(100, 3);

        String item = "test";
        Method addMethod = SimpleCountingBloomFilter.class.getMethod("add", Object.class);
        addMethod.invoke(filter, item);

        // Access private counters array
        Field countersField = SimpleCountingBloomFilter.class.getDeclaredField("counters");
        countersField.setAccessible(true);
        int[] counters = (int[]) countersField.get(filter);

        // Check that the expected counters are incremented
        for (int i = 0; i < 3; i++) {
            int index = computeHash(item, i, 100);
            assertTrue(counters[index] > 0, "Counter at index " + index + " should be incremented");
        }
    }

    @Order(8)
    @Test
    void mightContainReturnsTrueForAddedItem() throws Exception {
        Constructor<?> constructor = SimpleCountingBloomFilter.class.getDeclaredConstructor(int.class, int.class);
        Object filter = constructor.newInstance(100, 3);

        String item = "test";
        Method addMethod = SimpleCountingBloomFilter.class.getMethod("add", Object.class);
        addMethod.invoke(filter, item);

        Method mightContainMethod = SimpleCountingBloomFilter.class.getMethod("mightContain", Object.class);
        boolean result = (boolean) mightContainMethod.invoke(filter, item);

        assertTrue(result, "mightContain should return true for an added item");
    }

    @Order(9)
    @Test
    void mightContainReturnsFalseForNonAddedItem() throws Exception {
        Constructor<?> constructor = SimpleCountingBloomFilter.class.getDeclaredConstructor(int.class, int.class);
        Object filter = constructor.newInstance(100, 3);

        String item = "test";
        String notAdded = "notAdded";
        Method addMethod = SimpleCountingBloomFilter.class.getMethod("add", Object.class);
        addMethod.invoke(filter, item);

        Method mightContainMethod = SimpleCountingBloomFilter.class.getMethod("mightContain", Object.class);
        boolean result = (boolean) mightContainMethod.invoke(filter, notAdded);

        assertFalse(result, "mightContain should return false for a non-added item (unless a false positive occurs)");
    }

    @Order(10)
    @Test
    void removeDecrementsCounters() throws Exception {
        Constructor<?> constructor = SimpleCountingBloomFilter.class.getDeclaredConstructor(int.class, int.class);
        Object filter = constructor.newInstance(100, 3);

        String item = "test";
        Method addMethod = SimpleCountingBloomFilter.class.getMethod("add", Object.class);
        addMethod.invoke(filter, item);

        Method removeMethod = SimpleCountingBloomFilter.class.getMethod("remove", Object.class);
        removeMethod.invoke(filter, item);

        // Access private counters array
        Field countersField = SimpleCountingBloomFilter.class.getDeclaredField("counters");
        countersField.setAccessible(true);
        int[] counters = (int[]) countersField.get(filter);

        // Check that the expected counters are decremented
        for (int i = 0; i < 3; i++) {
            int index = computeHash(item, i, 100);
            assertEquals(0, counters[index], "Counter at index " + index + " should be decremented to zero");
        }
    }

    // Helper to compute hash as in the filter
    private int computeHash(String item, int i, int size) {
        int hashCode = (item == null ? 0 : item.hashCode());
        int combined = hashCode + i * 17;
        return Math.abs(combined) % size;
    }
}