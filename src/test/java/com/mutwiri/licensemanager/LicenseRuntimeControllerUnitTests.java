/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.controllers.LicenseRuntimeController;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.MachineStatus;
import com.mutwiri.licensemanager.entities.RuntimeTokenScope;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.services.ClientTokenService;
import com.mutwiri.licensemanager.services.CryptoService;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import com.mutwiri.licensemanager.services.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LicenseRuntimeControllerUnitTests {
    private final LicensePlatformService platformService = mock(LicensePlatformService.class);
    private final ClientTokenService clientTokenService = mock(ClientTokenService.class);
    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final CryptoService cryptoService = mock(CryptoService.class);
    private final LicenseRuntimeController controller = new LicenseRuntimeController(
            platformService, clientTokenService, rateLimitService, cryptoService);

    @Test
    void shouldAuthorizeAndExecuteEveryRuntimeWorkflow() {
        LocalDateTime now = LocalDateTime.now();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("203.0.113.9");
        servletRequest.setRequestURI("/api/v1/runtime/licenses/validate");

        ApiPayloads.ValidationRequest validationRequest = new ApiPayloads.ValidationRequest(
                "lic-key", "prod", "policy", "fingerprint", "1.0.0");
        ApiPayloads.MachineResponse machine = new ApiPayloads.MachineResponse(
                1L, "hash", "Laptop", "Linux", "1.0.0", MachineStatus.ACTIVE, now, now);
        ApiPayloads.ValidationResponse validation = new ApiPayloads.ValidationResponse(
                true, "VALID", "Valid", "lic-key", LicenseStatus.ACTIVE, "prod", "policy",
                List.of("feature"), machine, now.plusDays(30), now.plusMinutes(10));
        ApiPayloads.ActivationRequest activationRequest = new ApiPayloads.ActivationRequest(
                "fingerprint", "Laptop", "Linux", "1.0.0");
        ApiPayloads.HeartbeatRequest heartbeatRequest = new ApiPayloads.HeartbeatRequest("fingerprint", "1.0.1");
        ApiPayloads.OfflineCheckoutRequest checkoutRequest = new ApiPayloads.OfflineCheckoutRequest(
                "lic-key", "fingerprint", 3);
        ApiPayloads.OfflineLicenseResponse checkout = new ApiPayloads.OfflineLicenseResponse(
                "payload.signature", now, now.plusDays(3));
        ApiPayloads.OfflineVerifyRequest verifyRequest = new ApiPayloads.OfflineVerifyRequest("payload.signature");
        ApiPayloads.OfflineVerifyResponse verifyResponse = new ApiPayloads.OfflineVerifyResponse(
                true, "VALID", "Valid", "lic-key", "hash", now.plusDays(3));

        when(platformService.validate(validationRequest)).thenReturn(validation);
        when(platformService.activate("lic-key", activationRequest)).thenReturn(machine);
        when(platformService.heartbeat("lic-key", heartbeatRequest)).thenReturn(machine);
        when(platformService.deactivate("lic-key", "fingerprint")).thenReturn(machine);
        when(platformService.checkoutOffline(checkoutRequest)).thenReturn(checkout);
        when(platformService.verifyOffline(verifyRequest)).thenReturn(verifyResponse);
        when(cryptoService.offlinePublicKeyBase64()).thenReturn("public-key");

        assertThat(controller.validate("client-key", null, servletRequest, validationRequest)).isEqualTo(validation);
        assertThat(controller.activate("client-key", null, servletRequest, "lic-key", activationRequest).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(controller.heartbeat("client-key", null, servletRequest, "lic-key", heartbeatRequest)).isEqualTo(machine);
        assertThat(controller.deactivate("client-key", null, servletRequest, "lic-key", heartbeatRequest)).isEqualTo(machine);
        assertThat(controller.checkoutOffline("client-key", null, servletRequest, checkoutRequest)).isEqualTo(checkout);
        assertThat(controller.verifyOffline("client-key", null, servletRequest, verifyRequest)).isEqualTo(verifyResponse);
        assertThat(controller.offlinePublicKey("client-key", null, servletRequest).publicKeyBase64()).isEqualTo("public-key");

        verify(rateLimitService, times(7)).check("203.0.113.9:/api/v1/runtime/licenses/validate");
        verify(clientTokenService).requireRuntimeToken("client-key", RuntimeTokenScope.LICENSE_VALIDATE);
        verify(clientTokenService).requireRuntimeToken("client-key", RuntimeTokenScope.MACHINE_ACTIVATE);
        verify(clientTokenService).requireRuntimeToken("client-key", RuntimeTokenScope.MACHINE_HEARTBEAT);
        verify(clientTokenService).requireRuntimeToken("client-key", RuntimeTokenScope.MACHINE_DEACTIVATE);
        verify(clientTokenService).requireRuntimeToken("client-key", RuntimeTokenScope.OFFLINE_CHECKOUT);
        verify(clientTokenService).requireRuntimeToken("client-key", RuntimeTokenScope.OFFLINE_VERIFY);
        verify(clientTokenService).requireRuntimeToken("client-key", RuntimeTokenScope.OFFLINE_PUBLIC_KEY);
    }

    @Test
    void shouldAcceptBearerTokenAndRejectMissingRuntimeToken() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/runtime");

        ApiPayloads.ValidationRequest request = new ApiPayloads.ValidationRequest(
                "lic-key", null, null, null, null);
        ApiPayloads.ValidationResponse response = new ApiPayloads.ValidationResponse(
                false, "NOT_FOUND", "missing", "lic-key", null, null, null, List.of(), null, null, null);
        when(platformService.validate(request)).thenReturn(response);

        assertThat(controller.validate(null, "Bearer bearer-token", servletRequest, request)).isEqualTo(response);
        verify(clientTokenService).requireRuntimeToken("bearer-token", RuntimeTokenScope.LICENSE_VALIDATE);

        org.mockito.Mockito.doThrow(new ForbiddenException("missing")).when(clientTokenService)
                .requireRuntimeToken(null, RuntimeTokenScope.LICENSE_VALIDATE);
        assertThatThrownBy(() -> controller.validate(null, "Basic bad", servletRequest, request))
                .isInstanceOf(ForbiddenException.class);
    }
}
