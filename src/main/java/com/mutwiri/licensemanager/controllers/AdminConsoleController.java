/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.controllers;

import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.repository.AuditEventRepository;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.MachineRepository;
import com.mutwiri.licensemanager.repository.OrganizationMembershipRepository;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.PolicyRepository;
import com.mutwiri.licensemanager.repository.ProductRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminConsoleController {
    private final ProductRepository productRepository;
    private final PolicyRepository policyRepository;
    private final LicenseRepository licenseRepository;
    private final MachineRepository machineRepository;
    private final AuditEventRepository auditEventRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public AdminConsoleController(ProductRepository productRepository,
            PolicyRepository policyRepository,
            LicenseRepository licenseRepository,
            MachineRepository machineRepository,
            AuditEventRepository auditEventRepository,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.policyRepository = policyRepository;
        this.licenseRepository = licenseRepository;
        this.machineRepository = machineRepository;
        this.auditEventRepository = auditEventRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/admin")
    public String adminConsole(@AuthenticationPrincipal OAuth2User principal, Model model) {
        requireAdmin(principal);
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("policies", policyRepository.findAll());
        model.addAttribute("licenses", licenseRepository.findAll());
        model.addAttribute("machines", machineRepository.findAll());
        model.addAttribute("organizations", organizationRepository.findAll());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("memberships", membershipRepository.findAll());
        model.addAttribute("auditEvents", auditEventRepository.findTop100ByOrderByCreatedAtDesc());
        return "admin";
    }

    private void requireAdmin(OAuth2User principal) {
        if (principal == null) {
            throw new ForbiddenException("Admin console requires authentication.");
        }
        Object providerId = principal.getAttribute("sub") != null
                ? principal.getAttribute("sub")
                : principal.getAttribute("id");
        boolean admin = providerId != null
                && userRepository.findByProviderId(providerId.toString())
                .map(user -> user.getRole() == UserRole.ADMIN)
                .orElse(false);
        if (!admin) {
            throw new ForbiddenException("Admin console requires an ADMIN user.");
        }
    }
}
