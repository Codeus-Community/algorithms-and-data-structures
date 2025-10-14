package org.codeus.ws_deque;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSample {
  @Order(1)
  @Test
  void test() {
    assertTrue(true);
  }
}
