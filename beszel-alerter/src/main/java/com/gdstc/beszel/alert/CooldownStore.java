package com.gdstc.beszel.alert;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cooldown keyed by projectName + IP + metric + direction.
 */
@Component
public class CooldownStore {

    private final ConcurrentHashMap<String, Instant> lastFired = new ConcurrentHashMap<>();

    /**
     * @return true if the alert may be sent now (and records the send time); false if still cooling down
     */
    public boolean tryAcquire(String key, int cooldownSeconds) {
        Instant now = Instant.now();
        Instant previous = lastFired.get(key);
        if (previous != null && now.isBefore(previous.plusSeconds(cooldownSeconds))) {
            return false;
        }
        lastFired.put(key, now);
        return true;
    }

    public boolean isCoolingDown(String key, int cooldownSeconds) {
        Instant previous = lastFired.get(key);
        if (previous == null) {
            return false;
        }
        return Instant.now().isBefore(previous.plusSeconds(cooldownSeconds));
    }

    public void clear() {
        lastFired.clear();
    }

    /** For tests: set last-fired time explicitly. */
    void put(String key, Instant when) {
        lastFired.put(key, when);
    }
}
