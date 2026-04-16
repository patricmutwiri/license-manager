/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.controllers.AdminConsoleController;
import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.Machine;
import com.mutwiri.licensemanager.entities.MachineStatus;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.OrganizationMembership;
import com.mutwiri.licensemanager.entities.User;
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
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.ConcurrentModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminConsoleControllerUnitTests {

    @Test
    void shouldCalculateOperationalConsoleMetricsAndRequireAdmins() {
        Repositories repositories = new Repositories();
        AdminConsoleController controller = repositories.controller();
        Organization organization = new Organization();
        organization.setId(10L);
        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(organization);
        User admin = user(UserRole.ADMIN);
        admin.setProviderId("admin-sub");

        when(repositories.userRepository.findByProviderId("admin-sub")).thenReturn(Optional.of(admin));
        when(repositories.licenseRepository.findAll()).thenReturn(List.of(license(LicenseStatus.ACTIVE),
                license(LicenseStatus.SUSPENDED), license(LicenseStatus.EXPIRED), license(LicenseStatus.REVOKED)));
        when(repositories.machineRepository.findAll()).thenReturn(List.of(machine(MachineStatus.ACTIVE),
                machine(MachineStatus.HEARTBEAT_MISSED)));
        when(repositories.membershipRepository.findAll()).thenReturn(List.of(membership));
        when(repositories.productRepository.findAll()).thenReturn(List.of());
        when(repositories.policyRepository.findAll()).thenReturn(List.of());
        when(repositories.organizationRepository.findAll()).thenReturn(List.of(organization));
        when(repositories.userRepository.findAll()).thenReturn(List.of(admin));
        when(repositories.auditEventRepository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(repositories.clientApiTokenRepository.findAll()).thenReturn(List.of());
        when(repositories.billingPlanRepository.findAll()).thenReturn(List.of());
        when(repositories.billingSubscriptionRepository.findAll()).thenReturn(List.of());

        ConcurrentModel model = new ConcurrentModel();

        assertThat(controller.adminConsole(principal(Map.of("sub", "admin-sub")), model)).isEqualTo("admin");
        assertThat(model.getAttribute("activeLicenseCount")).isEqualTo(1L);
        assertThat(model.getAttribute("riskLicenseCount")).isEqualTo(3L);
        assertThat(model.getAttribute("activeMachineCount")).isEqualTo(1L);
        assertThat(model.getAttribute("missedHeartbeatCount")).isEqualTo(1L);
        assertThat((Map<Long, Long>) model.getAttribute("organizationMemberCounts")).containsEntry(10L, 1L);

        assertThatThrownBy(() -> controller.adminConsole(null, new ConcurrentModel()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.adminConsole(principal(Map.of("id", "unknown")), new ConcurrentModel()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.adminConsole(principal(Map.of()), new ConcurrentModel()))
                .isInstanceOf(ForbiddenException.class);
    }

    private License license(LicenseStatus status) {
        License license = new License();
        license.setStatus(status);
        return license;
    }

    private Machine machine(MachineStatus status) {
        Machine machine = new Machine();
        machine.setStatus(status);
        return machine;
    }

    private User user(UserRole role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    private OAuth2User principal(Map<String, Object> attributes) {
        OAuth2User principal = mock(OAuth2User.class);
        attributes.forEach((key, value) -> when(principal.getAttribute(key)).thenReturn(value));
        return principal;
    }

    private static final class Repositories {
        private final ProductRepository productRepository = mock(ProductRepository.class);
        private final PolicyRepository policyRepository = mock(PolicyRepository.class);
        private final LicenseRepository licenseRepository = mock(LicenseRepository.class);
        private final MachineRepository machineRepository = mock(MachineRepository.class);
        private final AuditEventRepository auditEventRepository = mock(AuditEventRepository.class);
        private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        private final OrganizationMembershipRepository membershipRepository = mock(OrganizationMembershipRepository.class);
        private final UserRepository userRepository = mock(UserRepository.class);
        private final ClientApiTokenRepository clientApiTokenRepository = mock(ClientApiTokenRepository.class);
        private final BillingPlanRepository billingPlanRepository = mock(BillingPlanRepository.class);
        private final BillingSubscriptionRepository billingSubscriptionRepository = mock(BillingSubscriptionRepository.class);

        private AdminConsoleController controller() {
            return new AdminConsoleController(productRepository, policyRepository, licenseRepository, machineRepository,
                    auditEventRepository, organizationRepository, membershipRepository, userRepository,
                    clientApiTokenRepository, billingPlanRepository, billingSubscriptionRepository);
        }
    }
}
