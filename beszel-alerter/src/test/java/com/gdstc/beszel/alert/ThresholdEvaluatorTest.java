package com.gdstc.beszel.alert;

import com.gdstc.beszel.beszel.SystemMetrics;
import com.gdstc.beszel.config.MonitorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThresholdEvaluatorTest {

    private ThresholdEvaluator evaluator;
    private MonitorProperties props;
    private MonitorProperties.ProjectConfig project;

    @BeforeEach
    void setUp() {
        evaluator = new ThresholdEvaluator();
        props = new MonitorProperties();
        MonitorProperties.DefaultsConfig defaults = new MonitorProperties.DefaultsConfig();
        MonitorProperties.MetricThresholds thresholds = new MonitorProperties.MetricThresholds();
        thresholds.setCpu(new MonitorProperties.Bound(80.0, null));
        thresholds.setMemory(new MonitorProperties.Bound(85.0, null));
        thresholds.setDisk(new MonitorProperties.Bound(90.0, 5.0));
        defaults.setThresholds(thresholds);
        props.setDefaults(defaults);

        project = new MonitorProperties.ProjectConfig();
        project.setName("科研管理平台");
        MonitorProperties.BeszelConfig beszel = new MonitorProperties.BeszelConfig();
        beszel.setBaseUrl("https://example.com/beszel");
        beszel.setEmail("a@b.c");
        beszel.setPassword("x");
        project.setBeszel(beszel);
    }

    @Test
    void gtTriggersWhenAboveThreshold() {
        SystemMetrics system = new SystemMetrics("1", "app", "10.0.0.1", "up", 91.2, 50.0, 40.0);
        MonitorProperties.HostConfig host = host("应用节点1", "10.0.0.1");
        project.setHosts(List.of(host));

        List<AlertEvent> alerts = evaluator.evaluate(props, project, List.of(system));

        assertEquals(1, alerts.size());
        AlertEvent a = alerts.get(0);
        assertEquals(AlertEvent.Metric.CPU, a.getMetric());
        assertEquals(AlertEvent.Direction.GT, a.getDirection());
        assertEquals("科研管理平台", a.getProjectName());
        assertTrue(a.formatMessage().contains("CPU 91.2% > 80"));
    }

    @Test
    void ltTriggersWhenBelowThreshold() {
        SystemMetrics system = new SystemMetrics("1", "db", "10.0.0.2", "up", 10.0, 20.0, 3.0);
        project.setHosts(List.of(host("db", "10.0.0.2")));

        List<AlertEvent> alerts = evaluator.evaluate(props, project, List.of(system));

        assertEquals(1, alerts.size());
        assertEquals(AlertEvent.Metric.DISK, alerts.get(0).getMetric());
        assertEquals(AlertEvent.Direction.LT, alerts.get(0).getDirection());
    }

    @Test
    void nullBoundDirectionDoesNotTrigger() {
        SystemMetrics system = new SystemMetrics("1", "app", "10.0.0.1", "up", 1.0, 1.0, 50.0);
        project.setHosts(List.of(host(null, "10.0.0.1")));

        List<AlertEvent> alerts = evaluator.evaluate(props, project, List.of(system));

        assertTrue(alerts.isEmpty());
    }

    @Test
    void offlineWhenStatusNotUp() {
        SystemMetrics system = new SystemMetrics("1", "app", "10.0.0.1", "down", 10.0, 10.0, 10.0);
        project.setHosts(List.of(host("应用节点1", "10.0.0.1")));

        List<AlertEvent> alerts = evaluator.evaluate(props, project, List.of(system));

        assertEquals(1, alerts.size());
        assertEquals(AlertEvent.Metric.OFFLINE, alerts.get(0).getMetric());
        assertTrue(alerts.get(0).formatMessage().contains("主机离线"));
    }

    @Test
    void emptyHostsMonitorsAllSystems() {
        project.setHosts(List.of());
        SystemMetrics a = new SystemMetrics("1", "a", "10.0.0.1", "up", 90.0, 10.0, 10.0);
        SystemMetrics b = new SystemMetrics("2", "b", "10.0.0.2", "up", 90.0, 10.0, 10.0);

        List<AlertEvent> alerts = evaluator.evaluate(props, project, List.of(a, b));

        assertEquals(2, alerts.size());
        assertTrue(alerts.stream().allMatch(e -> e.getMetric() == AlertEvent.Metric.CPU));
    }

    @Test
    void nullHostsMonitorsAllSystems() {
        project.setHosts(null);
        SystemMetrics a = new SystemMetrics("1", "a", "10.0.0.1", "up", 90.0, 10.0, 10.0);

        List<AlertEvent> alerts = evaluator.evaluate(props, project, List.of(a));

        assertEquals(1, alerts.size());
    }

    @Test
    void hostThresholdOverridesDefaults() {
        MonitorProperties.HostConfig host = host("app", "10.0.0.1");
        MonitorProperties.MetricThresholds hostTh = new MonitorProperties.MetricThresholds();
        hostTh.setCpu(new MonitorProperties.Bound(95.0, null));
        host.setThresholds(hostTh);
        project.setHosts(List.of(host));

        SystemMetrics system = new SystemMetrics("1", "app", "10.0.0.1", "up", 90.0, 10.0, 10.0);
        assertTrue(evaluator.evaluate(props, project, List.of(system)).isEmpty());

        SystemMetrics hotter = new SystemMetrics("1", "app", "10.0.0.1", "up", 96.0, 10.0, 10.0);
        assertEquals(1, evaluator.evaluate(props, project, List.of(hotter)).size());
    }

    @Test
    void matchPrefersHostFieldThenName() {
        SystemMetrics byHost = new SystemMetrics("1", "wrong", "10.0.0.1", "up", 1.0, 1.0, 1.0);
        SystemMetrics byName = new SystemMetrics("2", "10.0.0.2", "other", "up", 1.0, 1.0, 1.0);

        assertEquals("1", ThresholdEvaluator.findMatchingSystem(List.of(byHost, byName), "10.0.0.1").orElseThrow().getId());
        assertEquals("2", ThresholdEvaluator.findMatchingSystem(List.of(byName), "10.0.0.2").orElseThrow().getId());
        assertEquals(Optional.empty(), ThresholdEvaluator.findMatchingSystem(List.of(byHost), "9.9.9.9"));
    }

    @Test
    void matchIsCaseInsensitive() {
        SystemMetrics s = new SystemMetrics("1", "Node-A", "10.0.0.1", "up", 1.0, 1.0, 1.0);
        assertTrue(ThresholdEvaluator.findMatchingSystem(List.of(s), "10.0.0.1").isPresent());
        assertTrue(ThresholdEvaluator.findMatchingSystem(List.of(s), "node-a").isPresent());
    }

    @Test
    void configuredHostMissingProducesOffline() {
        project.setHosts(List.of(host("ghost", "10.9.9.9")));
        SystemMetrics other = new SystemMetrics("1", "a", "10.0.0.1", "up", 1.0, 1.0, 1.0);

        List<AlertEvent> alerts = evaluator.evaluate(props, project, List.of(other));

        assertEquals(1, alerts.size());
        assertEquals(AlertEvent.Metric.OFFLINE, alerts.get(0).getMetric());
        assertEquals("missing", alerts.get(0).getStatus());
    }

    private static MonitorProperties.HostConfig host(String name, String ip) {
        MonitorProperties.HostConfig h = new MonitorProperties.HostConfig();
        h.setName(name);
        h.setIp(ip);
        return h;
    }
}
