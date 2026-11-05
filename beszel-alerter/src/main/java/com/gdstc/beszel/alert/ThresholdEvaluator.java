package com.gdstc.beszel.alert;

import com.gdstc.beszel.beszel.SystemMetrics;
import com.gdstc.beszel.config.MonitorConfigLoader;
import com.gdstc.beszel.config.MonitorProperties;
import com.gdstc.beszel.util.Strings;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Matches systems to configured hosts and evaluates gt/lt thresholds (plus offline).
 */
@Component
public class ThresholdEvaluator {

    /**
     * Evaluate one project against the systems returned from its Hub.
     */
    public List<AlertEvent> evaluate(MonitorProperties props, MonitorProperties.ProjectConfig project,
                                     List<SystemMetrics> systems) {
        List<AlertEvent> alerts = new ArrayList<>();
        String hub = project.getBeszel() != null ? project.getBeszel().getBaseUrl() : null;
        String baseUrl = hub != null ? hub.trim().replaceAll("/+$", "") : "";

        if (project.monitorsAllHosts()) {
            MonitorProperties.MetricThresholds thresholds =
                    MonitorConfigLoader.resolveThresholds(props, project, null);
            for (SystemMetrics system : systems) {
                alerts.addAll(evaluateSystem(project.getName(), baseUrl, system, null, thresholds));
            }
            return alerts;
        }

        for (MonitorProperties.HostConfig hostConfig : project.getHosts()) {
            Optional<SystemMetrics> matched = findMatchingSystem(systems, hostConfig.getIp());
            MonitorProperties.MetricThresholds thresholds =
                    MonitorConfigLoader.resolveThresholds(props, project, hostConfig);
            if (matched.isEmpty()) {
                // Configured host not found in Hub → treat as offline
                String display = hostConfig.getName() != null ? hostConfig.getName() : hostConfig.getIp();
                alerts.add(AlertEvent.offline(project.getName(), display, hostConfig.getIp(), baseUrl, "missing"));
                continue;
            }
            alerts.addAll(evaluateSystem(project.getName(), baseUrl, matched.get(), hostConfig, thresholds));
        }
        return alerts;
    }

    private List<AlertEvent> evaluateSystem(String projectName, String baseUrl, SystemMetrics system,
                                            MonitorProperties.HostConfig hostConfig,
                                            MonitorProperties.MetricThresholds thresholds) {
        List<AlertEvent> alerts = new ArrayList<>();
        String ip = hostConfig != null && hostConfig.getIp() != null
                ? hostConfig.getIp()
                : system.resolveIpOrHost();
        String display = resolveDisplayName(hostConfig, system);

        if (!system.isUp()) {
            alerts.add(AlertEvent.offline(projectName, display, ip, baseUrl, system.getStatus()));
            return alerts;
        }

        evaluateBound(alerts, projectName, display, ip, baseUrl, system.getStatus(),
                AlertEvent.Metric.CPU, system.getCpu(), thresholds.getCpu());
        evaluateBound(alerts, projectName, display, ip, baseUrl, system.getStatus(),
                AlertEvent.Metric.MEMORY, system.getMemoryPercent(), thresholds.getMemory());
        evaluateBound(alerts, projectName, display, ip, baseUrl, system.getStatus(),
                AlertEvent.Metric.DISK, system.getDiskPercent(), thresholds.getDisk());
        return alerts;
    }

    static void evaluateBound(List<AlertEvent> alerts, String projectName, String display, String ip,
                              String baseUrl, String status, AlertEvent.Metric metric,
                              Double value, MonitorProperties.Bound bound) {
        if (value == null || bound == null) {
            return;
        }
        if (bound.getGt() != null && value > bound.getGt()) {
            alerts.add(AlertEvent.threshold(projectName, display, ip, baseUrl, status,
                    metric, AlertEvent.Direction.GT, value, bound.getGt()));
        }
        if (bound.getLt() != null && value < bound.getLt()) {
            alerts.add(AlertEvent.threshold(projectName, display, ip, baseUrl, status,
                    metric, AlertEvent.Direction.LT, value, bound.getLt()));
        }
    }

    /**
     * Match configured IP against system host first, then name (case-insensitive trim).
     */
    public static Optional<SystemMetrics> findMatchingSystem(List<SystemMetrics> systems, String configuredIp) {
        if (configuredIp == null || Strings.isBlank(configuredIp) || systems == null) {
            return Optional.empty();
        }
        String needle = configuredIp.trim();
        for (SystemMetrics s : systems) {
            if (s.getHost() != null && needle.equalsIgnoreCase(s.getHost().trim())) {
                return Optional.of(s);
            }
        }
        for (SystemMetrics s : systems) {
            if (s.getName() != null && needle.equalsIgnoreCase(s.getName().trim())) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    private static String resolveDisplayName(MonitorProperties.HostConfig hostConfig, SystemMetrics system) {
        if (hostConfig != null && Strings.isNotBlank(hostConfig.getName())) {
            return hostConfig.getName();
        }
        if (Strings.isNotBlank(system.getName())) {
            return system.getName();
        }
        return system.resolveIpOrHost();
    }
}
