/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.LicenseService;
import com.mutwiri.licensemanager.services.OrganizationService;

@Controller
public class IndexController {

    private final LicenseService licenseService;
    private final OrganizationService organizationService;
    private final UserRepository userRepository;

    public IndexController(LicenseService licenseService,
            OrganizationService organizationService,
            UserRepository userRepository) {
        this.licenseService = licenseService;
        this.organizationService = organizationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("organizations", organizationService.getAllOrganizations());
        return "index";
    }

    @PostMapping("/organizations")
    public String createOrganization(@RequestParam String name, @RequestParam String domain) {
        organizationService.createOrganization(name, domain);
        return "redirect:/";
    }

    @PostMapping("/licenses/generate")
    public String generateLicense(@RequestParam Long orgId, @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        String sub = principal.getAttribute("sub") != null ? principal.getAttribute("sub").toString() : null;
        String idAttribute = principal.getAttribute("id") != null ? principal.getAttribute("id").toString() : null;
        String providerId = sub != null ? sub : idAttribute;

        if (providerId == null) {
            throw new IllegalArgumentException("Could not identify user from OAuth2 provider attributes");
        }

        User user = userRepository.findByProviderId(providerId)
                .orElseGet(() -> {
                    String email = principal.getAttribute("email");
                    if (email != null) {
                        return userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalStateException(
                                        "User not found by email or providerId in database"));
                    }
                    throw new IllegalStateException("User not found in database for providerId: " + providerId);
                });

        licenseService.generateLicense(user.getId(), orgId);
        return "redirect:/licenses?orgId=" + orgId;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/licenses")
    public String licenses(@RequestParam(required = false) Long orgId, Model model) {
        List<License> licenses = orgId != null
                ? licenseService.getLicensesByOrganization(orgId)
                : List.of();

        model.addAttribute("licenses", licenses);
        model.addAttribute("now", LocalDateTime.now());
        return "licenses";
    }
}
