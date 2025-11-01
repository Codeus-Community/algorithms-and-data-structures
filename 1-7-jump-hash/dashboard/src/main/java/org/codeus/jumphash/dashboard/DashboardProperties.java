package org.codeus.jumphash.dashboard;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class DashboardProperties {

    private String statsTopic = "demo.stats";

    public String getStatsTopic() {
        return statsTopic;
    }

    public void setStatsTopic(String statsTopic) {
        this.statsTopic = statsTopic;
    }
}
