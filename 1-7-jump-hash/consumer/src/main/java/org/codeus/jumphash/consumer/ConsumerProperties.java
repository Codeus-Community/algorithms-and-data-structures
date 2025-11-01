package org.codeus.jumphash.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class ConsumerProperties {

    private String eventsTopic = "demo.events";
    private String statsTopic = "demo.stats";
    private String instanceId = "local-instance";
    private long statsIntervalMs = 1_000L;
    private boolean unsafeDemo = false;
    private int processingDelayMs = 0;

    public String getEventsTopic() {
        return eventsTopic;
    }

    public void setEventsTopic(String eventsTopic) {
        this.eventsTopic = eventsTopic;
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

    public long getStatsIntervalMs() {
        return statsIntervalMs;
    }

    public void setStatsIntervalMs(long statsIntervalMs) {
        this.statsIntervalMs = statsIntervalMs;
    }

    public boolean isUnsafeDemo() {
        return unsafeDemo;
    }

    public void setUnsafeDemo(boolean unsafeDemo) {
        this.unsafeDemo = unsafeDemo;
    }

    public int getProcessingDelayMs() {
        return processingDelayMs;
    }

    public void setProcessingDelayMs(int processingDelayMs) {
        this.processingDelayMs = processingDelayMs;
    }
}
