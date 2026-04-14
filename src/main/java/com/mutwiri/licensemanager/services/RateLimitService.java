/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public RateLimitService(@Value("${license.rate-limit.runtime-per-minute:120}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public void check(String key) {
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
