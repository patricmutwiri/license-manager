/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.entities.RuntimeTokenScope;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.services.ClientTokenService;
import com.mutwiri.licensemanager.services.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ClientTokenServiceTests {
    @Autowired
    private ClientTokenService clientTokenService;

    @MockitoBean
    private EmailService emailService;

    @Test
    void shouldEnforceScopesRotateAndRevokeRuntimeTokens() {
        ApiPayloads.ClientTokenResponse created = clientTokenService.create(new ApiPayloads.CreateClientTokenRequest(
                "validation-only", null, null, Set.of(RuntimeTokenScope.LICENSE_VALIDATE), null));

        assertThat(clientTokenService.requireRuntimeToken(created.token(), RuntimeTokenScope.LICENSE_VALIDATE)
                .getLastUsedAt()).isNotNull();
        assertThatThrownBy(() -> clientTokenService.requireRuntimeToken(created.token(), RuntimeTokenScope.MACHINE_ACTIVATE))
                .isInstanceOf(ForbiddenException.class);

        ApiPayloads.ClientTokenResponse rotated = clientTokenService.rotate(created.id(),
                new ApiPayloads.RotateClientTokenRequest(Set.of(RuntimeTokenScope.MACHINE_ACTIVATE), null));

        assertThat(rotated.token()).isNotEqualTo(created.token());
        assertThat(clientTokenService.requireRuntimeToken(rotated.token(), RuntimeTokenScope.MACHINE_ACTIVATE)
                .getScopes()).containsExactly(RuntimeTokenScope.MACHINE_ACTIVATE);
        assertThatThrownBy(() -> clientTokenService.requireRuntimeToken(created.token(), RuntimeTokenScope.LICENSE_VALIDATE))
                .isInstanceOf(ForbiddenException.class);

        ApiPayloads.ClientTokenResponse revoked = clientTokenService.revoke(created.id());

        assertThat(revoked.active()).isFalse();
        assertThatThrownBy(() -> clientTokenService.requireRuntimeToken(rotated.token(), RuntimeTokenScope.MACHINE_ACTIVATE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectBlankExpiredUnknownAndMissingResourceTokens() {
        ApiPayloads.ClientTokenResponse expired = clientTokenService.create(new ApiPayloads.CreateClientTokenRequest(
                "expired", null, null, Set.of(RuntimeTokenScope.LICENSE_VALIDATE), LocalDateTime.now().minusMinutes(1)));

        assertThatThrownBy(() -> clientTokenService.requireRuntimeToken(null, RuntimeTokenScope.LICENSE_VALIDATE))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Valid X-License-Client-Key header is required.");
        assertThatThrownBy(() -> clientTokenService.requireRuntimeToken("   ", RuntimeTokenScope.LICENSE_VALIDATE))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Valid X-License-Client-Key header is required.");
        assertThatThrownBy(() -> clientTokenService.requireRuntimeToken("lct_missing", RuntimeTokenScope.LICENSE_VALIDATE))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Runtime client token is invalid.");
        assertThatThrownBy(() -> clientTokenService.requireRuntimeToken(expired.token(), RuntimeTokenScope.LICENSE_VALIDATE))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Runtime client token is expired.");
        assertThatThrownBy(() -> clientTokenService.create(new ApiPayloads.CreateClientTokenRequest(
                "bad-product", 404L, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> clientTokenService.create(new ApiPayloads.CreateClientTokenRequest(
                "bad-license", null, 404L, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> clientTokenService.rotate(404L,
                new ApiPayloads.RotateClientTokenRequest(Set.of(RuntimeTokenScope.LICENSE_VALIDATE), null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> clientTokenService.revoke(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
