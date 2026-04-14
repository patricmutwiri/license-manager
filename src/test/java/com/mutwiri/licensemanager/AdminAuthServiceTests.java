/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.services.AdminAuthService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAuthServiceTests {
    @Test
    void shouldRejectMissingOrWrongAdminApiKey() {
        AdminAuthService authService = new AdminAuthService("expected-secret");

        assertThatThrownBy(() -> authService.requireAdmin(null)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> authService.requireAdmin("wrong-secret")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldAcceptMatchingAdminApiKey() {
        AdminAuthService authService = new AdminAuthService("expected-secret");

        assertThatCode(() -> authService.requireAdmin("expected-secret")).doesNotThrowAnyException();
    }
}

