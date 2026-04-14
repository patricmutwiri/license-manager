/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.controllers;

import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runtime")
public class LicenseRuntimeController {
    private final LicensePlatformService platformService;

    public LicenseRuntimeController(LicensePlatformService platformService) {
        this.platformService = platformService;
    }

    @PostMapping("/licenses/validate")
    public ApiPayloads.ValidationResponse validate(@Valid @RequestBody ApiPayloads.ValidationRequest request) {
        return platformService.validate(request);
    }

    @PostMapping("/licenses/{licenseKey}/machines")
    public ResponseEntity<ApiPayloads.MachineResponse> activate(
            @PathVariable String licenseKey,
            @Valid @RequestBody ApiPayloads.ActivationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.activate(licenseKey, request));
    }

    @PostMapping("/licenses/{licenseKey}/machines/heartbeat")
    public ApiPayloads.MachineResponse heartbeat(
            @PathVariable String licenseKey,
            @Valid @RequestBody ApiPayloads.HeartbeatRequest request) {
        return platformService.heartbeat(licenseKey, request);
    }

    @DeleteMapping("/licenses/{licenseKey}/machines")
    public ApiPayloads.MachineResponse deactivate(
            @PathVariable String licenseKey,
            @Valid @RequestBody ApiPayloads.HeartbeatRequest request) {
        return platformService.deactivate(licenseKey, request.fingerprint());
    }

    @PostMapping("/offline/checkouts")
    public ApiPayloads.OfflineLicenseResponse checkoutOffline(
            @Valid @RequestBody ApiPayloads.OfflineCheckoutRequest request) {
        return platformService.checkoutOffline(request);
    }

    @PostMapping("/offline/verify")
    public ApiPayloads.OfflineVerifyResponse verifyOffline(
            @Valid @RequestBody ApiPayloads.OfflineVerifyRequest request) {
        return platformService.verifyOffline(request);
    }
}

