package com.gdstc.beszel.notify;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotifierRegistry {

    private final Map<String, NotifyChannel> byType = new HashMap<>();

    public NotifierRegistry(List<NotifyChannel> channels) {
        for (NotifyChannel channel : channels) {
            byType.put(channel.type(), channel);
        }
    }

    public NotifyChannel require(String type) {
        NotifyChannel channel = byType.get(type);
        if (channel == null) {
            throw new IllegalArgumentException("Unsupported notify channel type: " + type);
        }
        return channel;
    }
}
