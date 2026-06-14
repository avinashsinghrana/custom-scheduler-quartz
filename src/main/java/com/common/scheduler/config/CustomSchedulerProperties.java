package com.common.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "custom.scheduler")
public class CustomSchedulerProperties {

    /**
     * Map of Job Group configurations. The key is the group name (e.g., country code).
     */
    private Map<String, JobGroupConfig> groups = new HashMap<>();

    public Map<String, JobGroupConfig> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, JobGroupConfig> groups) {
        this.groups = groups;
    }

    public static class JobGroupConfig {
        /**
         * Optional cron expression to override the annotation's default cron for this specific group.
         */
        private String cron;

        /**
         * The timezone for the schedule, e.g., "Asia/Kolkata", "America/New_York".
         */
        private String timezone;

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }
    }
}
