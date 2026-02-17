/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 12:40 AM
 *
 */

package com.mutwiri.licensemanager.controllers;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.services.LicenseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping("/generate")
    public License generateLicense(@RequestParam Long userId, @RequestParam Long organizationId) {
        return licenseService.generateLicense(userId, organizationId);
    }

    @GetMapping("/validate")
    public boolean validateLicense(@RequestParam String key) {
        return licenseService.validateLicense(key);
    }

    @GetMapping("/user/{userId}")
    public List<License> getLicensesByUser(@PathVariable Long userId) {
        return licenseService.getLicensesByUser(userId);
    }

    @GetMapping("/org/{orgId}")
    public List<License> getLicensesByOrg(@PathVariable Long orgId) {
        return licenseService.getLicensesByOrganization(orgId);
    }
}
