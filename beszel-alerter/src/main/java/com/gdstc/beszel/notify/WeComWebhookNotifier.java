package com.gdstc.beszel.notify;

import com.gdstc.beszel.alert.AlertEvent;
import com.gdstc.beszel.config.MonitorProperties;
import com.gdstc.beszel.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise WeChat group robot webhook notifier ({@code msgtype: text}).
 * Retries once on failure.
 */
@Component
public class WeComWebhookNotifier implements NotifyChannel {

    public static final String TYPE = "wecom_webhook";

    private static final Logger log = LoggerFactory.getLogger(WeComWebhookNotifier.class);

    private final RestTemplate restTemplate;

    public WeComWebhookNotifier(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void send(MonitorProperties.ChannelConfig channelConfig, AlertEvent event) {
        if (channelConfig == null || Strings.isBlank(channelConfig.getWebhookUrl())) {
            throw new IllegalArgumentException("WeCom webhookUrl is required");
        }
        String message = event.formatMessage();
        Map<String, Object> text = new HashMap<String, Object>();
        text.put("content", message);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("msgtype", "text");
        body.put("text", text);

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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity(webhookUrl, new HttpEntity<Map<String, Object>>(body, headers), String.class);
    }
}
