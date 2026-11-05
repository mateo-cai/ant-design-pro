package com.gdstc.beszel.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownStoreTest {

    private CooldownStore store;

    @BeforeEach
    void setUp() {
        store = new CooldownStore();
    }

    @Test
    void firstAcquireSucceeds() {
        assertTrue(store.tryAcquire("科研管理平台|10.0.0.1|CPU|GT", 300));
    }

    @Test
    void secondAcquireWithinCooldownFails() {
        String key = "科研管理平台|10.0.0.1|CPU|GT";
        assertTrue(store.tryAcquire(key, 300));
        assertFalse(store.tryAcquire(key, 300));
        assertTrue(store.isCoolingDown(key, 300));
    }

    @Test
    void differentProjectIpMetricOrDirectionAreIndependent() {
        assertTrue(store.tryAcquire("A|10.0.0.1|CPU|GT", 300));
        assertTrue(store.tryAcquire("B|10.0.0.1|CPU|GT", 300));
        assertTrue(store.tryAcquire("A|10.0.0.2|CPU|GT", 300));
        assertTrue(store.tryAcquire("A|10.0.0.1|MEMORY|GT", 300));
        assertTrue(store.tryAcquire("A|10.0.0.1|CPU|LT", 300));
    }

    @Test
    void acquireSucceedsAfterCooldownExpires() {
        String key = "p|1.1.1.1|DISK|LT";
        store.put(key, Instant.now().minusSeconds(301));
        assertTrue(store.tryAcquire(key, 300));
    }

    @Test
    void alertEventCooldownKeyIncludesProjectIpMetricDirection() {
        AlertEvent event = AlertEvent.threshold(
                "科研管理平台", "app", "10.0.0.1", "https://hub", "up",
                AlertEvent.Metric.CPU, AlertEvent.Direction.GT, 91.0, 80.0);
        assertTrue(event.cooldownKey().startsWith("科研管理平台|10.0.0.1|CPU|GT"));
    }
}
