/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.services.RateLimitService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RedisRateLimitServiceTests {
    @Test
    void shouldEnforceLimitsWithRedisWhenRedisUrlIsConfigured() {
        String redisUrl = System.getenv("LICENSE_RATE_LIMIT_REDIS_URL");
        assumeTrue(redisUrl != null && !redisUrl.isBlank(), "LICENSE_RATE_LIMIT_REDIS_URL is not configured");

        RateLimitService rateLimitService = new RateLimitService(
                1, redisUrl, "license-manager:test:" + UUID.randomUUID(), false);

        try {
            rateLimitService.check("client-a:/validate");

            assertThatThrownBy(() -> rateLimitService.check("client-a:/validate"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Runtime API rate limit exceeded.");
        } finally {
            rateLimitService.close();
        }
    }
}
