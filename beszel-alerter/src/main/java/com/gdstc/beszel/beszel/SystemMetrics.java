package com.gdstc.beszel.beszel;

import com.gdstc.beszel.util.Strings;

/**
 * Snapshot of a Beszel system record used for alerting.
 */
public class SystemMetrics {

    private final String id;
    private final String name;
    private final String host;
    private final String status;
    private final Double cpu;
    private final Double memoryPercent;
    private final Double diskPercent;

    public SystemMetrics(String id, String name, String host, String status,
                         Double cpu, Double memoryPercent, Double diskPercent) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.status = status;
        this.cpu = cpu;
        this.memoryPercent = memoryPercent;
        this.diskPercent = diskPercent;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getStatus() {
        return status;
    }

    public Double getCpu() {
        return cpu;
    }

    public Double getMemoryPercent() {
        return memoryPercent;
    }

    public Double getDiskPercent() {
        return diskPercent;
    }

    public boolean isUp() {
        return status != null && "up".equalsIgnoreCase(status.trim());
    }

    /**
     * Prefer Beszel host field; fall back to name for display / matching identity.
     */
    public String resolveIpOrHost() {
        if (host != null && Strings.isNotBlank(host)) {
            return host.trim();
        }
        return name != null ? name.trim() : "";
    }
}
