/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.controllers;

import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.MachineStatus;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.repository.AuditEventRepository;
import com.mutwiri.licensemanager.repository.BillingPlanRepository;
import com.mutwiri.licensemanager.repository.BillingSubscriptionRepository;
import com.mutwiri.licensemanager.repository.ClientApiTokenRepository;
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

import java.util.stream.Collectors;

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
    private final ClientApiTokenRepository clientApiTokenRepository;
    private final BillingPlanRepository billingPlanRepository;
    private final BillingSubscriptionRepository billingSubscriptionRepository;

    public AdminConsoleController(ProductRepository productRepository,
            PolicyRepository policyRepository,
            LicenseRepository licenseRepository,
            MachineRepository machineRepository,
            AuditEventRepository auditEventRepository,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            UserRepository userRepository,
            ClientApiTokenRepository clientApiTokenRepository,
            BillingPlanRepository billingPlanRepository,
            BillingSubscriptionRepository billingSubscriptionRepository) {
        this.productRepository = productRepository;
        this.policyRepository = policyRepository;
        this.licenseRepository = licenseRepository;
        this.machineRepository = machineRepository;
        this.auditEventRepository = auditEventRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.clientApiTokenRepository = clientApiTokenRepository;
        this.billingPlanRepository = billingPlanRepository;
        this.billingSubscriptionRepository = billingSubscriptionRepository;
    }

    @GetMapping("/admin")
    public String adminConsole(@AuthenticationPrincipal OAuth2User principal, Model model) {
        requireAdmin(principal);
        var products = productRepository.findAll();
        var policies = policyRepository.findAll();
        var licenses = licenseRepository.findAll();
        var machines = machineRepository.findAll();
        var organizations = organizationRepository.findAll();
        var memberships = membershipRepository.findAll();

        model.addAttribute("products", products);
        model.addAttribute("policies", policies);
        model.addAttribute("licenses", licenses);
        model.addAttribute("machines", machines);
        model.addAttribute("organizations", organizations);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("memberships", memberships);
        model.addAttribute("auditEvents", auditEventRepository.findTop100ByOrderByCreatedAtDesc());
        model.addAttribute("clientTokens", clientApiTokenRepository.findAll());
        model.addAttribute("billingPlans", billingPlanRepository.findAll());
        model.addAttribute("billingSubscriptions", billingSubscriptionRepository.findAll());
        model.addAttribute("organizationMemberCounts", memberships.stream()
                .collect(Collectors.groupingBy(membership -> membership.getOrganization().getId(), Collectors.counting())));
        model.addAttribute("activeLicenseCount", licenses.stream()
                .filter(license -> license.getStatus() == LicenseStatus.ACTIVE)
                .count());
        model.addAttribute("riskLicenseCount", licenses.stream()
                .filter(license -> license.getStatus() == LicenseStatus.SUSPENDED
                        || license.getStatus() == LicenseStatus.EXPIRED
                        || license.getStatus() == LicenseStatus.REVOKED)
                .count());
        model.addAttribute("activeMachineCount", machines.stream()
                .filter(machine -> machine.getStatus() == MachineStatus.ACTIVE)
                .count());
        model.addAttribute("missedHeartbeatCount", machines.stream()
                .filter(machine -> machine.getStatus() == MachineStatus.HEARTBEAT_MISSED)
                .count());
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
