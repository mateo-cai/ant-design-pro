package com.gdstc.beszel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitorConfigLoaderTest {

    private MonitorConfigLoader loader;

    @BeforeEach
    void setUp() {
        loader = new MonitorConfigLoader(new ObjectMapper(), "./config/monitor-config.json");
    }

    @Test
    void loadsExampleMultiProjectConfig() throws Exception {
        Path example = Paths.get("config/monitor-config.example.json");
        MonitorProperties props = loader.load(example);

        assertEquals(2, props.getProjects().size());
        assertEquals("科研管理平台", props.getProjects().get(0).getName());
        assertEquals("另一业务平台", props.getProjects().get(1).getName());
        assertEquals("https://xm.gdstc.gd.gov.cn/beszel",
                props.getProjects().get(0).getBeszel().getBaseUrl());
        assertEquals(2, props.getProjects().get(0).getHosts().size());
        assertEquals("wecom", props.getDefaults().getChannel());
        assertEquals(60, props.getDefaults().getIntervalSeconds());
        assertEquals(300, props.getDefaults().getCooldownSeconds());
        assertNotNull(props.getChannels().get("wecom"));
        assertEquals("wecom_webhook", props.getChannels().get("wecom").getType());
    }

    @Test
    void resolveThresholdsInheritsDefaultsThenProjectThenHost() {
        MonitorProperties props = new MonitorProperties();
        MonitorProperties.MetricThresholds defaults = new MonitorProperties.MetricThresholds();
        defaults.setCpu(new MonitorProperties.Bound(80.0, null));
        defaults.setMemory(new MonitorProperties.Bound(85.0, null));
        defaults.setDisk(new MonitorProperties.Bound(90.0, 5.0));
        props.getDefaults().setThresholds(defaults);

        MonitorProperties.ProjectConfig project = new MonitorProperties.ProjectConfig();
        MonitorProperties.MetricThresholds projectTh = new MonitorProperties.MetricThresholds();
        projectTh.setCpu(new MonitorProperties.Bound(70.0, null));
        project.setThresholds(projectTh);

        MonitorProperties.HostConfig host = new MonitorProperties.HostConfig();
        MonitorProperties.MetricThresholds hostTh = new MonitorProperties.MetricThresholds();
        hostTh.setMemory(new MonitorProperties.Bound(60.0, null));
        host.setThresholds(hostTh);

        MonitorProperties.MetricThresholds merged =
                MonitorConfigLoader.resolveThresholds(props, project, host);

        assertEquals(70.0, merged.getCpu().getGt());
        assertEquals(60.0, merged.getMemory().getGt());
        assertEquals(90.0, merged.getDisk().getGt());
        assertEquals(5.0, merged.getDisk().getLt());
    }

    @Test
    void emptyHostsMeansMonitorAll() {
        MonitorProperties.ProjectConfig p = new MonitorProperties.ProjectConfig();
        p.setHosts(null);
        assertTrue(p.monitorsAllHosts());
        p.setHosts(Collections.<MonitorProperties.HostConfig>emptyList());
        assertTrue(p.monitorsAllHosts());

        MonitorProperties.HostConfig h = new MonitorProperties.HostConfig();
        h.setIp("1.1.1.1");
        p.setHosts(Collections.singletonList(h));
        assertFalse(p.monitorsAllHosts());
    }

    @Test
    void validateRejectsMissingProjectNameAndBaseUrl() {
        MonitorProperties props = new MonitorProperties();
        MonitorProperties.ChannelConfig ch = new MonitorProperties.ChannelConfig();
        ch.setType("wecom_webhook");
        ch.setWebhookUrl("http://example");
        props.getChannels().put("wecom", ch);

        MonitorProperties.ProjectConfig project = new MonitorProperties.ProjectConfig();
        project.setName(" ");
        MonitorProperties.BeszelConfig beszel = new MonitorProperties.BeszelConfig();
        beszel.setBaseUrl("https://x");
        beszel.setEmail("a");
        beszel.setPassword("b");
        project.setBeszel(beszel);
        props.setProjects(Collections.singletonList(project));

        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                loader.validate(props);
            }
        });

        project.setName("ok");
        beszel.setBaseUrl("");
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                loader.validate(props);
            }
        });
    }

    @Test
    void resolveIntervalAndChannelFallBackToDefaults() {
        MonitorProperties props = new MonitorProperties();
        props.getDefaults().setIntervalSeconds(90);
        props.getDefaults().setChannel("wecom");
        props.getDefaults().setCooldownSeconds(120);

        MonitorProperties.ProjectConfig project = new MonitorProperties.ProjectConfig();
        assertEquals(90, MonitorConfigLoader.resolveIntervalSeconds(props, project));
        assertEquals("wecom", MonitorConfigLoader.resolveChannel(props, project));
        assertEquals(120, MonitorConfigLoader.resolveCooldownSeconds(props, project));

        project.setIntervalSeconds(30);
        project.setChannel("other");
        project.setCooldownSeconds(10);
        assertEquals(30, MonitorConfigLoader.resolveIntervalSeconds(props, project));
        assertEquals("other", MonitorConfigLoader.resolveChannel(props, project));
        assertEquals(10, MonitorConfigLoader.resolveCooldownSeconds(props, project));
    }
}
