/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.services.LicenseService;

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
