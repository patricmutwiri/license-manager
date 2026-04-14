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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTests {
    @Test
    void shouldLimitRequestsPerRuntimeKeyPerMinute() {
        RateLimitService rateLimitService = new RateLimitService(2);

        rateLimitService.check("client-a:/validate");
        rateLimitService.check("client-a:/validate");

        assertThatThrownBy(() -> rateLimitService.check("client-a:/validate"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Runtime API rate limit exceeded.");
        assertThatCode(() -> rateLimitService.check("client-b:/validate")).doesNotThrowAnyException();
    }
}
