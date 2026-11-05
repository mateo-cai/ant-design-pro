package com.gdstc.beszel.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Root monitor configuration loaded from JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitorProperties {

    private Map<String, ChannelConfig> channels = new HashMap<>();
    private DefaultsConfig defaults = new DefaultsConfig();
    private List<ProjectConfig> projects = new ArrayList<>();

    public Map<String, ChannelConfig> getChannels() {
        return channels;
    }

    public void setChannels(Map<String, ChannelConfig> channels) {
        this.channels = channels != null ? channels : new HashMap<>();
    }

    public DefaultsConfig getDefaults() {
        return defaults;
    }

    public void setDefaults(DefaultsConfig defaults) {
        this.defaults = defaults != null ? defaults : new DefaultsConfig();
    }

    public List<ProjectConfig> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectConfig> projects) {
        this.projects = projects != null ? projects : new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChannelConfig {
        private String type;
        private String webhookUrl;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DefaultsConfig {
        private int intervalSeconds = 60;
        private String channel = "wecom";
        private int cooldownSeconds = 300;
        private MetricThresholds thresholds = new MetricThresholds();

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(int intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public int getCooldownSeconds() {
            return cooldownSeconds;
        }

        public void setCooldownSeconds(int cooldownSeconds) {
            this.cooldownSeconds = cooldownSeconds;
        }

        public MetricThresholds getThresholds() {
            return thresholds;
        }

        public void setThresholds(MetricThresholds thresholds) {
            this.thresholds = thresholds != null ? thresholds : new MetricThresholds();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectConfig {
        private String name;
        private BeszelConfig beszel;
        private Integer intervalSeconds;
        private String channel;
        private Integer cooldownSeconds;
        private MetricThresholds thresholds;
        private List<HostConfig> hosts;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BeszelConfig getBeszel() {
            return beszel;
        }

        public void setBeszel(BeszelConfig beszel) {
            this.beszel = beszel;
        }

        public Integer getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(Integer intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public Integer getCooldownSeconds() {
            return cooldownSeconds;
        }

        public void setCooldownSeconds(Integer cooldownSeconds) {
            this.cooldownSeconds = cooldownSeconds;
        }

        public MetricThresholds getThresholds() {
            return thresholds;
        }

        public void setThresholds(MetricThresholds thresholds) {
            this.thresholds = thresholds;
        }

        public List<HostConfig> getHosts() {
            return hosts;
        }

        public void setHosts(List<HostConfig> hosts) {
            this.hosts = hosts;
        }

        /** True when hosts is null or empty → monitor all systems on the Hub. */
        public boolean monitorsAllHosts() {
            return hosts == null || hosts.isEmpty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BeszelConfig {
        private String baseUrl;
        private String email;
        private String password;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HostConfig {
        private String name;
        private String ip;
        private MetricThresholds thresholds;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public MetricThresholds getThresholds() {
            return thresholds;
        }

        public void setThresholds(MetricThresholds thresholds) {
            this.thresholds = thresholds;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetricThresholds {
        private Bound cpu;
        private Bound memory;
        private Bound disk;

        public Bound getCpu() {
            return cpu;
        }

        public void setCpu(Bound cpu) {
            this.cpu = cpu;
        }

        public Bound getMemory() {
            return memory;
        }

        public void setMemory(Bound memory) {
            this.memory = memory;
        }

        public Bound getDisk() {
            return disk;
        }

        public void setDisk(Bound disk) {
            this.disk = disk;
        }

        /**
         * Merge overlay onto base: non-null fields in overlay win; for Bound, non-null gt/lt win.
         */
        public static MetricThresholds merge(MetricThresholds base, MetricThresholds overlay) {
            MetricThresholds result = new MetricThresholds();
            result.setCpu(Bound.merge(base != null ? base.getCpu() : null, overlay != null ? overlay.getCpu() : null));
            result.setMemory(Bound.merge(base != null ? base.getMemory() : null, overlay != null ? overlay.getMemory() : null));
            result.setDisk(Bound.merge(base != null ? base.getDisk() : null, overlay != null ? overlay.getDisk() : null));
            return result;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bound {
        private Double gt;
        private Double lt;

        public Bound() {
        }

        public Bound(Double gt, Double lt) {
            this.gt = gt;
            this.lt = lt;
        }

        public Double getGt() {
            return gt;
        }

        public void setGt(Double gt) {
            this.gt = gt;
        }

        public Double getLt() {
            return lt;
        }

        public void setLt(Double lt) {
            this.lt = lt;
        }

        public static Bound merge(Bound base, Bound overlay) {
            if (base == null && overlay == null) {
                return null;
            }
            Double gt = overlay != null && overlay.getGt() != null
                    ? overlay.getGt()
                    : (base != null ? base.getGt() : null);
            Double lt = overlay != null && overlay.getLt() != null
                    ? overlay.getLt()
                    : (base != null ? base.getLt() : null);
            // If overlay explicitly sets null via JSON null, Jackson sets the field to null —
            // for merge we treat missing overlay Bound as inherit; when Bound exists, null gt/lt means closed.
            if (overlay != null) {
                // Prefer overlay values even when null (direction closed), but only if the Bound object itself is present.
                // Jackson cannot distinguish "omitted" vs "null" without JsonNullable; keep simple inherit-then-override:
                // when overlay Bound is non-null, use its gt/lt as-is (including null = closed).
                return new Bound(overlay.getGt() != null ? overlay.getGt() : (base != null ? base.getGt() : null),
                        overlay.getLt() != null ? overlay.getLt() : (base != null ? base.getLt() : null));
            }
            return new Bound(gt, lt);
        }
    }
}
