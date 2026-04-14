/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.controllers;

import com.mutwiri.licensemanager.entities.RuntimeTokenScope;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.services.ClientTokenService;
import com.mutwiri.licensemanager.services.CryptoService;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import com.mutwiri.licensemanager.services.RateLimitService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runtime")
public class LicenseRuntimeController {
    private static final String CLIENT_KEY_HEADER = "X-License-Client-Key";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final LicensePlatformService platformService;
    private final ClientTokenService clientTokenService;
    private final RateLimitService rateLimitService;
    private final CryptoService cryptoService;

    public LicenseRuntimeController(LicensePlatformService platformService,
            ClientTokenService clientTokenService,
            RateLimitService rateLimitService,
            CryptoService cryptoService) {
        this.platformService = platformService;
        this.clientTokenService = clientTokenService;
        this.rateLimitService = rateLimitService;
        this.cryptoService = cryptoService;
    }

    @PostMapping("/licenses/validate")
    public ApiPayloads.ValidationResponse validate(
            @RequestHeader(value = CLIENT_KEY_HEADER, required = false) String clientKey,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorization,
            HttpServletRequest servletRequest,
            @Valid @RequestBody ApiPayloads.ValidationRequest request) {
        authorizeRuntime(clientKey, authorization, servletRequest, RuntimeTokenScope.LICENSE_VALIDATE);
        return platformService.validate(request);
    }

    @PostMapping("/licenses/{licenseKey}/machines")
    public ResponseEntity<ApiPayloads.MachineResponse> activate(
            @RequestHeader(value = CLIENT_KEY_HEADER, required = false) String clientKey,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorization,
            HttpServletRequest servletRequest,
            @PathVariable String licenseKey,
            @Valid @RequestBody ApiPayloads.ActivationRequest request) {
        authorizeRuntime(clientKey, authorization, servletRequest, RuntimeTokenScope.MACHINE_ACTIVATE);
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.activate(licenseKey, request));
    }

    @PostMapping("/licenses/{licenseKey}/machines/heartbeat")
    public ApiPayloads.MachineResponse heartbeat(
            @RequestHeader(value = CLIENT_KEY_HEADER, required = false) String clientKey,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorization,
            HttpServletRequest servletRequest,
            @PathVariable String licenseKey,
            @Valid @RequestBody ApiPayloads.HeartbeatRequest request) {
        authorizeRuntime(clientKey, authorization, servletRequest, RuntimeTokenScope.MACHINE_HEARTBEAT);
        return platformService.heartbeat(licenseKey, request);
    }

    @DeleteMapping("/licenses/{licenseKey}/machines")
    public ApiPayloads.MachineResponse deactivate(
            @RequestHeader(value = CLIENT_KEY_HEADER, required = false) String clientKey,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorization,
            HttpServletRequest servletRequest,
            @PathVariable String licenseKey,
            @Valid @RequestBody ApiPayloads.HeartbeatRequest request) {
        authorizeRuntime(clientKey, authorization, servletRequest, RuntimeTokenScope.MACHINE_DEACTIVATE);
        return platformService.deactivate(licenseKey, request.fingerprint());
    }

    @PostMapping("/offline/checkouts")
    public ApiPayloads.OfflineLicenseResponse checkoutOffline(
            @RequestHeader(value = CLIENT_KEY_HEADER, required = false) String clientKey,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorization,
            HttpServletRequest servletRequest,
            @Valid @RequestBody ApiPayloads.OfflineCheckoutRequest request) {
        authorizeRuntime(clientKey, authorization, servletRequest, RuntimeTokenScope.OFFLINE_CHECKOUT);
        return platformService.checkoutOffline(request);
    }

    @PostMapping("/offline/verify")
    public ApiPayloads.OfflineVerifyResponse verifyOffline(
            @RequestHeader(value = CLIENT_KEY_HEADER, required = false) String clientKey,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorization,
            HttpServletRequest servletRequest,
            @Valid @RequestBody ApiPayloads.OfflineVerifyRequest request) {
        authorizeRuntime(clientKey, authorization, servletRequest, RuntimeTokenScope.OFFLINE_VERIFY);
        return platformService.verifyOffline(request);
    }

    @GetMapping("/offline/public-key")
    public ApiPayloads.OfflinePublicKeyResponse offlinePublicKey(
            @RequestHeader(value = CLIENT_KEY_HEADER, required = false) String clientKey,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorization,
            HttpServletRequest servletRequest) {
        authorizeRuntime(clientKey, authorization, servletRequest, RuntimeTokenScope.OFFLINE_PUBLIC_KEY);
        return new ApiPayloads.OfflinePublicKeyResponse("Ed25519", cryptoService.offlinePublicKeyBase64());
    }

    private void authorizeRuntime(String clientKey, String authorization, HttpServletRequest servletRequest,
            RuntimeTokenScope requiredScope) {
        String remoteAddress = servletRequest.getRemoteAddr() == null ? "unknown" : servletRequest.getRemoteAddr();
        rateLimitService.check(remoteAddress + ":" + servletRequest.getRequestURI());
        clientTokenService.requireRuntimeToken(firstNonBlank(clientKey, bearerToken(authorization)), requiredScope);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
