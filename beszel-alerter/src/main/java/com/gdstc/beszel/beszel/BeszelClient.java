package com.gdstc.beszel.beszel;

import com.fasterxml.jackson.databind.JsonNode;
import com.gdstc.beszel.config.MonitorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PocketBase-backed Beszel Hub client with per-baseUrl token cache.
 */
@Component
public class BeszelClient {

    private static final Logger log = LoggerFactory.getLogger(BeszelClient.class);

    /** Refresh token slightly before PocketBase default ~14 day expiry; also re-auth on 401. */
    private static final long TOKEN_TTL_SECONDS = 12 * 60 * 60;

    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<String, CachedToken>();

    public BeszelClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<SystemMetrics> listSystems(MonitorProperties.BeszelConfig beszel) {
        String baseUrl = normalizeBaseUrl(beszel.getBaseUrl());
        String token = getToken(baseUrl, beszel);
        try {
            return fetchSystems(baseUrl, token);
        } catch (HttpStatusCodeException ex) {
            if (ex.getRawStatusCode() == 401) {
                log.warn("Beszel token expired for {}, re-authenticating", baseUrl);
                tokenCache.remove(baseUrl);
                token = authenticate(baseUrl, beszel);
                return fetchSystems(baseUrl, token);
            }
            throw ex;
        }
    }

    private List<SystemMetrics> fetchSystems(String baseUrl, String token) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/collections/systems/records")
                .queryParam("perPage", 200)
                .queryParam("fields", "id,name,host,status,info")
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<Void>(headers), JsonNode.class);

        List<SystemMetrics> result = new ArrayList<SystemMetrics>();
        JsonNode body = response.getBody();
        if (body == null || !body.has("items")) {
            return result;
        }
        for (JsonNode item : body.get("items")) {
            result.add(mapSystem(item));
        }
        return result;
    }

    static SystemMetrics mapSystem(JsonNode item) {
        String id = text(item, "id");
        String name = text(item, "name");
        String host = text(item, "host");
        String status = text(item, "status");
        JsonNode info = item.get("info");
        Double cpu = null;
        Double mp = null;
        Double dp = null;
        if (info != null && !info.isNull()) {
            cpu = asDouble(info.get("cpu"));
            mp = asDouble(info.get("mp"));
            dp = asDouble(info.get("dp"));
        }
        return new SystemMetrics(id, name, host, status, cpu, mp, dp);
    }

    private String getToken(String baseUrl, MonitorProperties.BeszelConfig beszel) {
        CachedToken cached = tokenCache.get(baseUrl);
        if (cached != null && Instant.now().isBefore(cached.expiresAt)) {
            return cached.token;
        }
        return authenticate(baseUrl, beszel);
    }

    private String authenticate(String baseUrl, MonitorProperties.BeszelConfig beszel) {
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("identity", beszel.getEmail());
        payload.put("password", beszel.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                baseUrl + "/api/collections/users/auth-with-password",
                new HttpEntity<Map<String, String>>(payload, headers),
                JsonNode.class);

        JsonNode body = response.getBody();
        if (body == null || !body.hasNonNull("token")) {
            throw new IllegalStateException("Beszel auth response missing token for " + baseUrl);
        }
        String token = body.get("token").asText();
        tokenCache.put(baseUrl, new CachedToken(token, Instant.now().plusSeconds(TOKEN_TTL_SECONDS)));
        log.info("Authenticated to Beszel Hub {}", baseUrl);
        return token;
    }

    static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Double asDouble(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        try {
            return Double.parseDouble(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Visible for tests: clear cached tokens. */
    void clearTokenCache() {
        tokenCache.clear();
    }

    private static final class CachedToken {
        private final String token;
        private final Instant expiresAt;

        private CachedToken(String token, Instant expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }
    }
}
