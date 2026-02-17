/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);
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

    @GetMapping("/licenses/generate")
    public String showGenerateForm(@RequestParam Long orgId, Model model) {
        model.addAttribute("orgId", orgId);
        model.addAttribute("defaultExpiry", LocalDateTime.now().plusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        return "generate";
    }

    @PostMapping("/licenses/generate")
    public String generateLicense(
            @RequestParam Long orgId,
            @RequestParam String applicationName,
            @RequestParam(required = false) String hostname,
            @RequestParam String email,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) List<String> customKeys,
            @RequestParam(required = false) List<String> customValues,
            @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        Object subObj = principal.getAttribute("sub");
        Object idObj = principal.getAttribute("id");
        String sub = subObj != null ? subObj.toString() : null;
        String idAttribute = idObj != null ? idObj.toString() : null;
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

        logger.info("Generating advanced license for orgId: {} by user: {} for app: {}", orgId, providerId,
                applicationName);

        LocalDateTime expiry = (expiryDate != null && !expiryDate.isEmpty())
                ? LocalDateTime.parse(expiryDate + "T00:00:00")
                : LocalDateTime.now().plusYears(1);

        Map<String, String> customFields = new HashMap<>();
        if (customKeys != null && customValues != null) {
            for (int i = 0; i < customKeys.size(); i++) {
                String key = customKeys.get(i);
                String value = i < customValues.size() ? customValues.get(i) : "";
                if (key != null && !key.trim().isEmpty()) {
                    customFields.put(key, value);
                }
            }
        }

        licenseService.generateLicense(user.getId(), orgId, hostname, applicationName, email, expiry, customFields);
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

        List<Map<String, Object>> licenseData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime now = LocalDateTime.now();

        for (License l : licenses) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("key", l.getKey());
            map.put("expiryFormatted", l.getExpiry() != null ? l.getExpiry().format(formatter) : "No Expiry");
            map.put("active", l.getExpiry() != null && l.getExpiry().isAfter(now));

            String name = "System User";
            if (l.getUser() != null) {
                name = l.getUser().getName() != null && !l.getUser().getName().trim().isEmpty()
                        ? l.getUser().getName()
                        : l.getUser().getEmail();
            }
            map.put("userName", name);
            licenseData.add(map);
        }

        model.addAttribute("licenses", licenseData);
        return "licenses";
    }
}
