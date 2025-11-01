package org.codeus.jumphash.common;

/**
 * Event payload emitted by the producer service.
 */
public record DemoEvent(String userId, String eventType, long timestamp) {
}
