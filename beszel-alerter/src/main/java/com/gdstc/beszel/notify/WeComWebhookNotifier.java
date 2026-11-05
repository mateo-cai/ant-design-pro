package com.gdstc.beszel.notify;

import com.gdstc.beszel.alert.AlertEvent;
import com.gdstc.beszel.config.MonitorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Enterprise WeChat group robot webhook notifier ({@code msgtype: text}).
 * Retries once on failure.
 */
@Component
public class WeComWebhookNotifier implements NotifyChannel {

    public static final String TYPE = "wecom_webhook";

    private static final Logger log = LoggerFactory.getLogger(WeComWebhookNotifier.class);

    private final RestClient.Builder restClientBuilder;

    public WeComWebhookNotifier(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void send(MonitorProperties.ChannelConfig channelConfig, AlertEvent event) {
        if (channelConfig == null || channelConfig.getWebhookUrl() == null || channelConfig.getWebhookUrl().isBlank()) {
            throw new IllegalArgumentException("WeCom webhookUrl is required");
        }
        String message = event.formatMessage();
        Map<String, Object> body = Map.of(
                "msgtype", "text",
                "text", Map.of("content", message));

        try {
            doPost(channelConfig.getWebhookUrl(), body);
        } catch (Exception first) {
            log.warn("WeCom send failed, retrying once: {}", first.toString());
            try {
                doPost(channelConfig.getWebhookUrl(), body);
            } catch (Exception second) {
                log.error("WeCom send failed after retry: {}", second.toString());
                throw new IllegalStateException("WeCom webhook failed after retry", second);
            }
        }
    }

    private void doPost(String webhookUrl, Map<String, Object> body) {
        restClientBuilder.build()
                .post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
