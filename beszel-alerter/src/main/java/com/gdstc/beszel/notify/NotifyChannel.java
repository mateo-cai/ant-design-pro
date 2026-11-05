package com.gdstc.beszel.notify;

import com.gdstc.beszel.alert.AlertEvent;
import com.gdstc.beszel.config.MonitorProperties;

/**
 * Pluggable notification channel (WeCom first; others reserved via config type).
 */
public interface NotifyChannel {

    String type();

    void send(MonitorProperties.ChannelConfig channelConfig, AlertEvent event);
}
