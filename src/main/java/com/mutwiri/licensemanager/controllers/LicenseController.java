/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 12:40 AM
 *
 */

package com.mutwiri.licensemanager.controllers;

import com.mutwiri.licensemanager.models.dto.DtoMapper;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.models.dto.LicenseResponse;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import com.mutwiri.licensemanager.services.LicenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for license operations.
 */
@RestController
@RequestMapping("/api/v1/licenses")
@Slf4j
public class LicenseController {

    private final LicenseService licenseService;
    private final DtoMapper dtoMapper;
    private final LicensePlatformService platformService;

    public LicenseController(LicenseService licenseService, DtoMapper dtoMapper, LicensePlatformService platformService) {
        this.licenseService = licenseService;
        this.dtoMapper = dtoMapper;
        this.platformService = platformService;
    }

    /**
     * Generate a new license.
     */
    @PostMapping
    public ResponseEntity<LicenseResponse> generateLicense(
            @RequestParam Long userId,
            @RequestParam Long organizationId) {
        log.info("Generating license for userId: {}, orgId: {}", userId, organizationId);
        var license = licenseService.generateLicense(userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dtoMapper.toLicenseResponse(license));
    }

    /**
     * Validate a license key.
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateLicense(@RequestParam String key) {
        log.debug("Validating license key: {}", key);
        ApiPayloads.ValidationResponse validation = platformService.validate(
                new ApiPayloads.ValidationRequest(key, null, null, null, null));
        return ResponseEntity.ok(Map.of(
                "key", key,
                "valid", validation.valid(),
                "code", validation.code(),
                "detail", validation.detail()));
    }

    /**
     * Get all licenses for a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LicenseResponse>> getLicensesByUser(@PathVariable Long userId) {
        log.info("Fetching licenses for userId: {}", userId);
        var licenses = licenseService.getLicensesByUser(userId);
        return ResponseEntity.ok(licenses.stream()
                .map(dtoMapper::toLicenseResponse)
                .toList());
    }

    /**
     * Get all licenses for an organization.
     */
    @GetMapping("/org/{orgId}")
    public ResponseEntity<List<LicenseResponse>> getLicensesByOrg(@PathVariable Long orgId) {
        log.info("Fetching licenses for orgId: {}", orgId);
        var licenses = licenseService.getLicensesByOrganization(orgId);
        return ResponseEntity.ok(licenses.stream()
                .map(dtoMapper::toLicenseResponse)
                .toList());
    }
}
