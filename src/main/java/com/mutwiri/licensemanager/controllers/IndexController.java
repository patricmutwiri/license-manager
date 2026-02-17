/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.services.LicenseService;
import com.mutwiri.licensemanager.services.OrganizationService;

@Controller
public class IndexController {

    private final LicenseService licenseService;
    private final OrganizationService organizationService;

    public IndexController(LicenseService licenseService, OrganizationService organizationService) {
        this.licenseService = licenseService;
        this.organizationService = organizationService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("organizations", organizationService.getAllOrganizations());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/licenses")
    public String licenses(@RequestParam(required = false) Long orgId, Model model) {
        List<License> licenses;
        if (orgId != null) {
            licenses = licenseService.getLicensesByOrganization(orgId);
        } else {
            // In a real app, this would be filtered by current user
            // For now, let's just show an empty list or some demo data
            licenses = List.of();
        }
        model.addAttribute("licenses", licenses);
        return "licenses";
    }
}
