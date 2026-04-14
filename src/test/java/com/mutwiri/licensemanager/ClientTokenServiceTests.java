/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.entities.RuntimeTokenScope;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.services.ClientTokenService;
import com.mutwiri.licensemanager.services.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

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
}
