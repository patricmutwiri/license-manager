/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.LicenseService;
import com.mutwiri.licensemanager.services.OrganizationService;

import jakarta.persistence.EntityNotFoundException;

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
    public String index(Model model, @AuthenticationPrincipal OAuth2User principal) {
        model.addAttribute("organizations", organizationService.getAllOrganizations());
        if (principal != null) {
            String name = principal.getAttribute("name");
            if (name == null) {
                name = principal.getAttribute("login"); // GitHub fallback
            }
            if (name == null) {
                name = "User"; // Generic fallback
            }
            model.addAttribute("userName", name);
        }
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

        User user = resolveUser(principal);

        logger.info("Generating advanced license for orgId: {} by user: {} for app: {}",
                orgId, user.getEmail(), applicationName);

        LocalDateTime expiry = parseExpiryDate(expiryDate);
        Map<String, String> customFields = parseCustomFields(customKeys, customValues);

        licenseService.generateLicense(user.getId(), orgId, hostname, applicationName, email, expiry, customFields);
        return "redirect:/licenses?orgId=" + orgId;
    }

    private User resolveUser(OAuth2User principal) {
        String providerId = extractProviderId(principal);
        return userRepository.findByProviderId(providerId)
                .orElseGet(() -> {
                    String userEmail = principal.getAttribute("email");
                    if (userEmail != null) {
                        return userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new IllegalStateException("User not found by email or providerId"));
                    }
                    throw new IllegalStateException("User not found for providerId: " + providerId);
                });
    }

    private String extractProviderId(OAuth2User principal) {
        Object sub = principal.getAttribute("sub");
        Object id = principal.getAttribute("id");

        String providerId = null;
        if (sub != null) {
            providerId = sub.toString();
        } else if (id != null) {
            providerId = id.toString();
        }

        if (providerId == null) {
            throw new IllegalArgumentException("Could not identify user from OAuth2 provider attributes");
        }
        return providerId;
    }

    private LocalDateTime parseExpiryDate(String expiryDate) {
        return (expiryDate != null && !expiryDate.isEmpty())
                ? LocalDateTime.parse(expiryDate + "T00:00:00")
                : LocalDateTime.now().plusYears(1);
    }

    private Map<String, String> parseCustomFields(List<String> keys, List<String> values) {
        Map<String, String> customFields = new HashMap<>();
        if (keys != null && values != null) {
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                String value = i < values.size() ? values.get(i) : "";
                if (key != null && !key.trim().isEmpty()) {
                    customFields.put(key, value);
                }
            }
        }
        return customFields;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/licenses")
    public String licenses(@RequestParam(required = false) Long orgId, Model model) {
        if (orgId != null) {
            Organization organization = organizationService.getOrganizationById(orgId)
                    .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + orgId));
            model.addAttribute("organization", organization);
        }

        List<License> licenses = orgId != null
                ? licenseService.getLicensesByOrganization(orgId)
                : List.of();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime now = LocalDateTime.now();

        try {
            List<Map<String, Object>> licenseData = licenses.stream()
                    .map(l -> transformToLicenseData(l, formatter, now))
                    .toList();

            model.addAttribute("licenses", licenseData);
            model.addAttribute("orgId", orgId); // Ensure orgId is available for links
            return "licenses";
        } catch (Exception e) {
            e.printStackTrace(); // FORCE PRINT TO CONSOLE
            throw e;
        }
    }

    private Map<String, Object> transformToLicenseData(License l, DateTimeFormatter formatter, LocalDateTime now) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", l.getId());
        map.put("key", l.getKey());
        map.put("expiryFormatted", l.getExpiry() != null ? l.getExpiry().format(formatter) : "No Expiry");
        map.put("active", l.getExpiry() != null && l.getExpiry().isAfter(now));
        map.put("userName", resolveLicenseUserName(l));
        return map;
    }

    private String resolveLicenseUserName(License l) {
        if (l.getUser() == null) {
            return "System User";
        }
        return (l.getUser().getName() != null && !l.getUser().getName().trim().isEmpty())
                ? l.getUser().getName()
                : l.getUser().getEmail();
    }
}
