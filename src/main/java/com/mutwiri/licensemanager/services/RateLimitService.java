/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> redisConnection;
    private final HttpClient upstashClient;
    private final URI upstashBaseUri;
    private final String upstashToken;
    private final String redisKeyPrefix;
    private final boolean failOpen;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public RateLimitService(
            @Value("${license.rate-limit.runtime-per-minute:120}") int requestsPerMinute,
            @Value("${license.rate-limit.redis-url:}") String redisUrl,
            @Value("${license.rate-limit.redis-key-prefix:license-manager:rate-limit}") String redisKeyPrefix,
            @Value("${license.rate-limit.fail-open:true}") boolean failOpen) {
        this.requestsPerMinute = requestsPerMinute;
        this.redisKeyPrefix = redisKeyPrefix;
        this.failOpen = failOpen;
        if (redisUrl == null || redisUrl.isBlank()) {
            this.redisClient = null;
            this.redisConnection = null;
            this.upstashClient = null;
            this.upstashBaseUri = null;
            this.upstashToken = null;
            return;
        }
        URI uri = URI.create(redisUrl);
        if (isUpstash(uri)) {
            this.redisClient = null;
            this.redisConnection = null;
            this.upstashClient = HttpClient.newHttpClient();
            this.upstashBaseUri = URI.create("https://" + uri.getHost());
            this.upstashToken = passwordFrom(uri);
            return;
        }
        this.upstashClient = null;
        this.upstashBaseUri = null;
        this.upstashToken = null;
        this.redisClient = RedisClient.create(RedisURI.create(redisUrl));
        this.redisConnection = redisClient.connect();
    }

    public RateLimitService(int requestsPerMinute, URI upstashBaseUri, String upstashToken,
            String redisKeyPrefix, boolean failOpen) {
        this.requestsPerMinute = requestsPerMinute;
        this.redisKeyPrefix = redisKeyPrefix;
        this.failOpen = failOpen;
        this.redisClient = null;
        this.redisConnection = null;
        this.upstashClient = HttpClient.newHttpClient();
        this.upstashBaseUri = upstashBaseUri;
        this.upstashToken = upstashToken;
    }

    public void check(String key) {
        if (upstashClient != null) {
            checkUpstash(key);
            return;
        }
        if (redisConnection != null) {
            checkRedis(key);
            return;
        }
        checkLocal(key);
    }

    @PreDestroy
    public void close() {
        if (redisConnection != null) {
            redisConnection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    private void checkRedis(String key) {
        try {
            RedisCommands<String, String> commands = redisConnection.sync();
            String redisKey = redisKeyPrefix + ":" + key + ":" + Instant.now().getEpochSecond() / 60;
            long count = commands.incr(redisKey);
            if (count == 1) {
                commands.expire(redisKey, 70);
            }
            if (count > requestsPerMinute) {
                throw new ForbiddenException("Runtime API rate limit exceeded.");
            }
        } catch (ForbiddenException e) {
            throw e;
        } catch (RuntimeException e) {
            if (!failOpen) {
                throw new ForbiddenException("Runtime API rate limiter is unavailable.");
            }
            checkLocal(key);
        }
    }

    private void checkUpstash(String key) {
        try {
            String redisKey = redisKeyPrefix + ":" + key + ":" + Instant.now().getEpochSecond() / 60;
            String encodedKey = encodePath(redisKey);
            long count = upstashLong("incr/" + encodedKey);
            if (count == 1) {
                upstashLong("expire/" + encodedKey + "/70");
            }
            if (count > requestsPerMinute) {
                throw new ForbiddenException("Runtime API rate limit exceeded.");
            }
        } catch (ForbiddenException e) {
            throw e;
        } catch (RuntimeException e) {
            if (!failOpen) {
                throw new ForbiddenException("Runtime API rate limiter is unavailable.");
            }
            checkLocal(key);
        }
    }

    private long upstashLong(String commandPath) {
        try {
            HttpRequest request = HttpRequest.newBuilder(upstashBaseUri.resolve("/" + commandPath))
                    .header("Authorization", "Bearer " + upstashToken)
                    .GET()
                    .build();
            HttpResponse<String> response = upstashClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Upstash returned HTTP " + response.statusCode());
            }
            Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            Object result = body.get("result");
            if (result instanceof Number number) {
                return number.longValue();
            }
            if (result instanceof String text) {
                return Long.parseLong(text);
            }
            throw new IllegalStateException("Upstash response did not include a numeric result.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while contacting Upstash Redis.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to contact Upstash Redis.", e);
        }
    }

    private boolean isUpstash(URI uri) {
        return uri.getHost() != null && uri.getHost().endsWith(".upstash.io");
    }

    private String passwordFrom(URI uri) {
        String userInfo = uri.getRawUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalArgumentException("Upstash Redis URL must include a password token.");
        }
        return URLDecoder.decode(userInfo.substring(userInfo.indexOf(':') + 1), StandardCharsets.UTF_8);
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void checkLocal(String key) {
        long minute = Instant.now().getEpochSecond() / 60;
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            if (existing == null || existing.minute != minute) {
                return new Bucket(minute, 1);
            }
            return new Bucket(minute, existing.count + 1);
        });
        if (bucket.count > requestsPerMinute) {
            throw new ForbiddenException("Runtime API rate limit exceeded.");
        }
    }

    private record Bucket(long minute, int count) {
    }
}
