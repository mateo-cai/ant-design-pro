package com.gdstc.beszel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads and validates {@link MonitorProperties} from an external JSON file.
 */
@Component
public class MonitorConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(MonitorConfigLoader.class);

    private final ObjectMapper objectMapper;
    private final String configPath;

    public MonitorConfigLoader(ObjectMapper objectMapper,
                               @Value("${monitor.config:./config/monitor-config.json}") String configPath) {
        this.objectMapper = objectMapper;
        this.configPath = configPath;
    }

    public String getConfigPath() {
        return configPath;
    }

    public MonitorProperties load() throws IOException {
        return load(Path.of(configPath));
    }

    public MonitorProperties load(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("Monitor config not found: " + path.toAbsolutePath());
        }
        MonitorProperties props = objectMapper.readValue(path.toFile(), MonitorProperties.class);
        validate(props);
        log.info("Loaded monitor config from {} ({} project(s))", path.toAbsolutePath(), props.getProjects().size());
        return props;
    }

    public void validate(MonitorProperties props) {
        if (props == null) {
            throw new IllegalArgumentException("Monitor config is null");
        }
        if (props.getProjects() == null || props.getProjects().isEmpty()) {
            throw new IllegalArgumentException("projects must not be empty");
        }
        Set<String> names = new HashSet<>();
        for (int i = 0; i < props.getProjects().size(); i++) {
            MonitorProperties.ProjectConfig project = props.getProjects().get(i);
            if (project.getName() == null || project.getName().isBlank()) {
                throw new IllegalArgumentException("projects[" + i + "].name is required");
            }
            if (!names.add(project.getName())) {
                throw new IllegalArgumentException("duplicate project name: " + project.getName());
            }
            if (project.getBeszel() == null) {
                throw new IllegalArgumentException("projects[" + i + "].beszel is required");
            }
            if (project.getBeszel().getBaseUrl() == null || project.getBeszel().getBaseUrl().isBlank()) {
                throw new IllegalArgumentException("projects[" + i + "].beszel.baseUrl is required");
            }
            if (project.getBeszel().getEmail() == null || project.getBeszel().getEmail().isBlank()) {
                throw new IllegalArgumentException("projects[" + i + "].beszel.email is required");
            }
            if (project.getBeszel().getPassword() == null || project.getBeszel().getPassword().isBlank()) {
                throw new IllegalArgumentException("projects[" + i + "].beszel.password is required");
            }
            if (project.getHosts() != null) {
                for (int j = 0; j < project.getHosts().size(); j++) {
                    MonitorProperties.HostConfig host = project.getHosts().get(j);
                    if (host.getIp() == null || host.getIp().isBlank()) {
                        throw new IllegalArgumentException(
                                "projects[" + i + "].hosts[" + j + "].ip is required when hosts are listed");
                    }
                }
            }
            String channel = resolveChannel(props, project);
            if (!props.getChannels().containsKey(channel)) {
                throw new IllegalArgumentException(
                        "projects[" + i + "] channel '" + channel + "' not found in channels");
            }
        }
    }

    public static String resolveChannel(MonitorProperties props, MonitorProperties.ProjectConfig project) {
        if (project.getChannel() != null && !project.getChannel().isBlank()) {
            return project.getChannel();
        }
        return props.getDefaults().getChannel();
    }

    public static int resolveIntervalSeconds(MonitorProperties props, MonitorProperties.ProjectConfig project) {
        if (project.getIntervalSeconds() != null) {
            return project.getIntervalSeconds();
        }
        return props.getDefaults().getIntervalSeconds();
    }

    public static int resolveCooldownSeconds(MonitorProperties props, MonitorProperties.ProjectConfig project) {
        if (project.getCooldownSeconds() != null) {
            return project.getCooldownSeconds();
        }
        return props.getDefaults().getCooldownSeconds();
    }

    public static MonitorProperties.MetricThresholds resolveThresholds(
            MonitorProperties props,
            MonitorProperties.ProjectConfig project,
            MonitorProperties.HostConfig host) {
        MonitorProperties.MetricThresholds merged =
                MonitorProperties.MetricThresholds.merge(props.getDefaults().getThresholds(), project.getThresholds());
        if (host != null) {
            merged = MonitorProperties.MetricThresholds.merge(merged, host.getThresholds());
        }
        return merged;
    }
}
