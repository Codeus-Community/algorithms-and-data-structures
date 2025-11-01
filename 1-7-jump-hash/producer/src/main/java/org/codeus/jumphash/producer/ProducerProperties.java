package org.codeus.jumphash.producer;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public class ProducerProperties {

    private String topic = "demo.events";
    private int eventsPerSecond = 300;
    private int userPoolSize = 5000;
    private List<String> eventTypes = List.of("CLICK", "VIEW", "PURCHASE", "LOGIN", "LOGOUT");
    private long tickIntervalMs = 1000L;
    private long statsIntervalMs = 1000L;
    private String statsTopic = "demo.stats";
    private String instanceId = "producer-instance";

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getEventsPerSecond() {
        return eventsPerSecond;
    }

    public void setEventsPerSecond(int eventsPerSecond) {
        this.eventsPerSecond = eventsPerSecond;
    }

    public int getUserPoolSize() {
        return userPoolSize;
    }

    public void setUserPoolSize(int userPoolSize) {
        this.userPoolSize = userPoolSize;
    }

    public List<String> getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(List<String> eventTypes) {
        this.eventTypes = eventTypes;
    }

    public long getTickIntervalMs() {
        return tickIntervalMs;
    }

    public void setTickIntervalMs(long tickIntervalMs) {
        this.tickIntervalMs = tickIntervalMs;
    }

    public long getStatsIntervalMs() {
        return statsIntervalMs;
    }

    public void setStatsIntervalMs(long statsIntervalMs) {
        this.statsIntervalMs = statsIntervalMs;
    }

    public String getStatsTopic() {
        return statsTopic;
    }

    public void setStatsTopic(String statsTopic) {
        this.statsTopic = statsTopic;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
