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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> redisConnection;
    private final String redisKeyPrefix;
    private final boolean failOpen;

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
            return;
        }
        this.redisClient = RedisClient.create(RedisURI.create(redisUrl));
        this.redisConnection = redisClient.connect();
    }

    public void check(String key) {
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
