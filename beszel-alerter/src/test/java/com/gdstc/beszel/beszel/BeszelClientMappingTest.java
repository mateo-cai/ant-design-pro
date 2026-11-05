package com.gdstc.beszel.beszel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeszelClientMappingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsInfoMetricsAndStatus() throws Exception {
        JsonNode item = mapper.readTree("""
                {
                  "id": "abc",
                  "name": "app-1",
                  "host": "10.0.0.1",
                  "status": "up",
                  "info": { "cpu": 12.5, "mp": 40.1, "dp": 70.0 }
                }
                """);
        SystemMetrics m = BeszelClient.mapSystem(item);
        assertEquals("abc", m.getId());
        assertEquals("app-1", m.getName());
        assertEquals("10.0.0.1", m.getHost());
        assertTrue(m.isUp());
        assertEquals(12.5, m.getCpu());
        assertEquals(40.1, m.getMemoryPercent());
        assertEquals(70.0, m.getDiskPercent());
    }

    @Test
    void normalizeBaseUrlStripsTrailingSlash() {
        assertEquals("https://example.com/beszel", BeszelClient.normalizeBaseUrl("https://example.com/beszel/"));
        assertEquals("https://example.com/beszel", BeszelClient.normalizeBaseUrl("https://example.com/beszel"));
    }

    @Test
    void downStatusIsNotUp() throws Exception {
        JsonNode item = mapper.readTree("""
                {"id":"1","name":"x","host":"1.1.1.1","status":"down","info":{}}
                """);
        assertFalse(BeszelClient.mapSystem(item).isUp());
    }
}
