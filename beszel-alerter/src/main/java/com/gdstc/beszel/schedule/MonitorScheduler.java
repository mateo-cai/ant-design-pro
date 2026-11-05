package com.gdstc.beszel.schedule;

import com.gdstc.beszel.alert.AlertEvent;
import com.gdstc.beszel.alert.CooldownStore;
import com.gdstc.beszel.alert.ThresholdEvaluator;
import com.gdstc.beszel.beszel.BeszelClient;
import com.gdstc.beszel.beszel.SystemMetrics;
import com.gdstc.beszel.config.MonitorConfigLoader;
import com.gdstc.beszel.config.MonitorProperties;
import com.gdstc.beszel.notify.NotifierRegistry;
import com.gdstc.beszel.notify.NotifyChannel;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Schedules one polling task per project, respecting each project's intervalSeconds.
 */
@Component
public class MonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitorScheduler.class);

    private final MonitorConfigLoader configLoader;
    private final BeszelClient beszelClient;
    private final ThresholdEvaluator thresholdEvaluator;
    private final CooldownStore cooldownStore;
    private final NotifierRegistry notifierRegistry;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private volatile MonitorProperties properties;

    public MonitorScheduler(MonitorConfigLoader configLoader,
                            BeszelClient beszelClient,
                            ThresholdEvaluator thresholdEvaluator,
                            CooldownStore cooldownStore,
                            NotifierRegistry notifierRegistry) {
        this.configLoader = configLoader;
        this.beszelClient = beszelClient;
        this.thresholdEvaluator = thresholdEvaluator;
        this.cooldownStore = cooldownStore;
        this.notifierRegistry = notifierRegistry;
    }

    @PostConstruct
    public void start() {
        try {
            properties = configLoader.load();
        } catch (Exception e) {
            log.error("Failed to load monitor config from {}: {}. Scheduler not started.",
                    configLoader.getConfigPath(), e.toString());
            return;
        }
        for (MonitorProperties.ProjectConfig project : properties.getProjects()) {
            int interval = MonitorConfigLoader.resolveIntervalSeconds(properties, project);
            ScheduledFuture<?> future = executor.scheduleWithFixedDelay(
                    () -> pollProjectSafe(project.getName()),
                    0,
                    Math.max(interval, 5),
                    TimeUnit.SECONDS);
            futures.put(project.getName(), future);
            log.info("Scheduled project '{}' every {}s", project.getName(), interval);
        }
    }

    @PreDestroy
    public void stop() {
        futures.values().forEach(f -> f.cancel(false));
        executor.shutdownNow();
    }

    private void pollProjectSafe(String projectName) {
        try {
            pollProject(projectName);
        } catch (Exception e) {
            log.error("Poll failed for project '{}': {}", projectName, e.toString());
        }
    }

    void pollProject(String projectName) {
        MonitorProperties props = this.properties;
        if (props == null) {
            return;
        }
        MonitorProperties.ProjectConfig project = props.getProjects().stream()
                .filter(p -> projectName.equals(p.getName()))
                .findFirst()
                .orElse(null);
        if (project == null) {
            return;
        }

        List<SystemMetrics> systems = beszelClient.listSystems(project.getBeszel());
        log.debug("Project '{}' fetched {} system(s)", projectName, systems.size());

        List<AlertEvent> alerts = thresholdEvaluator.evaluate(props, project, systems);
        int cooldown = MonitorConfigLoader.resolveCooldownSeconds(props, project);
        String channelName = MonitorConfigLoader.resolveChannel(props, project);
        MonitorProperties.ChannelConfig channelConfig = props.getChannels().get(channelName);
        if (channelConfig == null) {
            log.error("Channel '{}' missing for project '{}'", channelName, projectName);
            return;
        }
        NotifyChannel notifier = notifierRegistry.require(channelConfig.getType());

        for (AlertEvent alert : alerts) {
            if (!cooldownStore.tryAcquire(alert.cooldownKey(), cooldown)) {
                log.debug("Cooldown skip: {}", alert.cooldownKey());
                continue;
            }
            try {
                notifier.send(channelConfig, alert);
                log.info("Alert sent: {}", alert.cooldownKey());
            } catch (Exception e) {
                log.error("Notify failed for {}: {}", alert.cooldownKey(), e.toString());
            }
        }
    }
}
