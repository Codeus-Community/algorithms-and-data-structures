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
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleBloomFilterTest {

    @Order(1)
    @Test
    void numHashesConstantIsPresent() throws NoSuchFieldException, IllegalAccessException {
        Field numHashesField = SimpleBloomFilter.class.getDeclaredField("NUM_HASHES");
        assertTrue(Modifier.isPrivate(numHashesField.getModifiers()), "NUM_HASHES should be private");
        assertTrue(Modifier.isStatic(numHashesField.getModifiers()), "NUM_HASHES should be static");
        assertTrue(Modifier.isFinal(numHashesField.getModifiers()), "NUM_HASHES should be final");
        assertEquals(int.class, numHashesField.getType(), "NUM_HASHES should be of type int");
        numHashesField.setAccessible(true);
        assertEquals(3, numHashesField.get(null), "NUM_HASHES should be 3");
    }

    @Order(2)
    @Test
    void bitsFieldIsPresent() throws NoSuchFieldException {
        Field bitsField = SimpleBloomFilter.class.getDeclaredField("bits");
        assertTrue(Modifier.isPrivate(bitsField.getModifiers()), "bits should be private");
        assertEquals(boolean[].class, bitsField.getType(), "bits should be of type boolean[]");
    }

    @Order(3)
    @Test
    void sizeFieldIsPresent() throws NoSuchFieldException {
        Field sizeField = SimpleBloomFilter.class.getDeclaredField("size");
        assertTrue(Modifier.isPrivate(sizeField.getModifiers()), "size should be private");
        assertEquals(int.class, sizeField.getType(), "size should be of type int");
    }

    @Order(4)
    @Test
    void onlyOneIntConstructorIsPresent() {
        Constructor<?>[] constructors = SimpleBloomFilter.class.getDeclaredConstructors();
        assertEquals(1, constructors.length, "There should be exactly one constructor");
        Constructor<?> constructor = constructors[0];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        assertEquals(1, parameterTypes.length, "Constructor should have exactly one parameter");
        assertEquals(int.class, parameterTypes[0], "Constructor parameter should be of type int");
    }

    @Order(5)
    @Test
    void addSetsCorrectBits() throws Exception {
        Constructor<?> constructor = SimpleBloomFilter.class.getDeclaredConstructor(int.class);
        Object filter = constructor.newInstance(100);

        String item = "test";

        SimpleBloomFilter.class.getMethod("add", Object.class).invoke(filter, item);

        Field bitsField = SimpleBloomFilter.class.getDeclaredField("bits");
        bitsField.setAccessible(true);
        boolean[] bits = (boolean[]) bitsField.get(filter);

        for (int i = 0; i < 3; i++) {
            int index = computeHash(item, i, 100);
            assertTrue(bits[index], "Bit at index " + index + " should be set to true");
        }
    }

    private int computeHash(String item, int i, int size) {
        int hashCode = (item == null ? 0 : item.hashCode());
        int combined = hashCode + i * 17;
        return Math.abs(combined) % size;
    }

    @Order(6)
    @Test
    void mightContainReturnsTrueForAddedItem() throws Exception {
        Constructor<?> constructor = SimpleBloomFilter.class.getDeclaredConstructor(int.class);
        Object filter = constructor.newInstance(100);

        String item = "test";

        Method addMethod = SimpleBloomFilter.class.getMethod("add", Object.class);
        addMethod.invoke(filter, item);

        Method mightContainMethod = SimpleBloomFilter.class.getMethod("mightContain", Object.class);
        boolean result = (boolean) mightContainMethod.invoke(filter, item);

        assertTrue(result, "mightContain should return true for an added item");
    }

    @Order(7)
    @Test
    void mightContainReturnsFalseForNonAddedItem() throws Exception {
        Constructor<?> constructor = SimpleBloomFilter.class.getDeclaredConstructor(int.class);
        Object filter = constructor.newInstance(100);

        String item = "test";
        String notAdded = "notAdded";

        Method addMethod = SimpleBloomFilter.class.getMethod("add", Object.class);
        addMethod.invoke(filter, item);

        Method mightContainMethod = SimpleBloomFilter.class.getMethod("mightContain", Object.class);
        boolean result = (boolean) mightContainMethod.invoke(filter, notAdded);

        // It is possible for a Bloom filter to have a false positive, but for a small filter and a single item, this is unlikely
        // For demonstration, we expect false
        assertFalse(result, "mightContain should return false for a non-added item (unless a false positive occurs)");
    }
}