package com.gdstc.beszel.alert;

import com.gdstc.beszel.util.Strings;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A single alert to be pushed (threshold breach or offline).
 */
public class AlertEvent {

    public enum Metric {
        CPU("CPU"),
        MEMORY("内存"),
        DISK("磁盘"),
        OFFLINE("离线");

        private final String label;

        Metric(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Direction {
        GT(">"),
        LT("<"),
        OFFLINE("offline");

        private final String symbol;

        Direction(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String projectName;
    private final String hostDisplayName;
    private final String ip;
    private final String hubBaseUrl;
    private final String status;
    private final Metric metric;
    private final Direction direction;
    private final Double value;
    private final Double threshold;
    private final LocalDateTime time;

    public AlertEvent(String projectName, String hostDisplayName, String ip, String hubBaseUrl,
                      String status, Metric metric, Direction direction,
                      Double value, Double threshold, LocalDateTime time) {
        this.projectName = projectName;
        this.hostDisplayName = hostDisplayName;
        this.ip = ip;
        this.hubBaseUrl = hubBaseUrl;
        this.status = status;
        this.metric = metric;
        this.direction = direction;
        this.value = value;
        this.threshold = threshold;
        this.time = time != null ? time : LocalDateTime.now();
    }

    public String getProjectName() {
        return projectName;
    }

    public String getHostDisplayName() {
        return hostDisplayName;
    }

    public String getIp() {
        return ip;
    }

    public String getHubBaseUrl() {
        return hubBaseUrl;
    }

    public String getStatus() {
        return status;
    }

    public Metric getMetric() {
        return metric;
    }

    public Direction getDirection() {
        return direction;
    }

    public Double getValue() {
        return value;
    }

    public Double getThreshold() {
        return threshold;
    }

    public LocalDateTime getTime() {
        return time;
    }

    /** Cooldown key: projectName + IP + metric + direction. */
    public String cooldownKey() {
        return projectName + "|" + ip + "|" + metric.name() + "|" + direction.name();
    }

    public String formatMessage() {
        String hostLine = (Strings.isNotBlank(hostDisplayName))
                ? hostDisplayName + " (" + ip + ")"
                : ip;
        String metricLine;
        if (metric == Metric.OFFLINE) {
            metricLine = "指标: 主机离线";
        } else {
            metricLine = String.format("指标: %s %.1f%% %s %.0f",
                    metric.getLabel(),
                    value != null ? value : 0.0,
                    direction.getSymbol(),
                    threshold != null ? threshold : 0.0);
        }
        return String.join("\n",
                "【Beszel告警】" + projectName,
                "主机: " + hostLine,
                metricLine,
                "Hub: " + hubBaseUrl,
                "状态: " + (status != null ? status : "unknown"),
                "时间: " + TIME_FMT.format(time));
    }

    public static AlertEvent offline(String projectName, String hostDisplayName, String ip,
                                     String hubBaseUrl, String status) {
        return new AlertEvent(projectName, hostDisplayName, ip, hubBaseUrl, status,
                Metric.OFFLINE, Direction.OFFLINE, null, null, LocalDateTime.now());
    }

    public static AlertEvent threshold(String projectName, String hostDisplayName, String ip,
                                       String hubBaseUrl, String status,
                                       Metric metric, Direction direction,
                                       double value, double threshold) {
        return new AlertEvent(projectName, hostDisplayName, ip, hubBaseUrl, status,
                metric, direction, value, threshold, LocalDateTime.now());
    }
}
