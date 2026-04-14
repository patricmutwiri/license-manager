/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.LicensingModel;
import com.mutwiri.licensemanager.entities.Machine;
import com.mutwiri.licensemanager.entities.MachineStatus;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.OrganizationRole;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.exceptions.ConflictException;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.repository.MachineRepository;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.EmailService;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LicensePlatformServiceTests {
    @Autowired
    private LicensePlatformService platformService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private MachineRepository machineRepository;

    @MockitoBean
    private EmailService emailService;

    private User user;
    private Organization organization;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Platform User");
        user.setEmail("platform-user@example.com");
        user.setProvider("test");
        user.setProviderId("platform-user");
        user = userRepository.save(user);

        organization = new Organization();
        organization.setName("Platform Org");
        organization.setEmail("platform-org@example.com");
        organization.setDomain("platform.example.com");
        organization = organizationRepository.save(organization);
    }

    @Test
    void shouldCreateAndListUsersAndOrganizationsForAdminWorkflows() {
        ApiPayloads.UserResponse customer = platformService.createUser(new ApiPayloads.CreateUserRequest(
                "API Customer", "api-customer@example.com", UserRole.CUSTOMER, null, null));
        ApiPayloads.OrganizationResponse customerOrg = platformService.createOrganization(
                new ApiPayloads.CreateOrganizationRequest("API Customer Org", "billing@example.com", "api-customer.example.com"));

        assertThat(customer.role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(platformService.listUsers()).extracting(ApiPayloads.UserResponse::email)
                .contains("api-customer@example.com");
        assertThat(customerOrg.domain()).isEqualTo("api-customer.example.com");
        assertThat(platformService.listOrganizations()).extracting(ApiPayloads.OrganizationResponse::domain)
                .contains("api-customer.example.com");
    }

    @Test
    void shouldEnforceOrganizationMembershipPermissions() {
        ApiPayloads.UserResponse owner = platformService.createUser(new ApiPayloads.CreateUserRequest(
                "Owner User", "owner@example.com", UserRole.CUSTOMER, null, null));
        ApiPayloads.UserResponse viewer = platformService.createUser(new ApiPayloads.CreateUserRequest(
                "Viewer User", "viewer@example.com", UserRole.CUSTOMER, null, null));
        ApiPayloads.OrganizationResponse customerOrg = platformService.createOrganization(
                new ApiPayloads.CreateOrganizationRequest("RBAC Org", "rbac@example.com", "rbac.example.com"));

        ApiPayloads.MembershipResponse ownerMembership = platformService.createMembership(null,
                new ApiPayloads.CreateMembershipRequest(owner.id(), customerOrg.id(), OrganizationRole.OWNER));
        platformService.createMembership(owner.id(),
                new ApiPayloads.CreateMembershipRequest(viewer.id(), customerOrg.id(), OrganizationRole.VIEWER));
        ApiPayloads.PolicyResponse policy = createPolicy(LicensingModel.NODE_LOCKED, 1, 1);

        assertThat(ownerMembership.permissions()).contains(com.mutwiri.licensemanager.entities.Permission.LICENSE_ISSUE);
        assertThatThrownBy(() -> platformService.issueLicense(viewer.id(), new ApiPayloads.IssueLicenseRequest(
                viewer.id(), customerOrg.id(), policy.id(), "Viewer", "viewer@example.com",
                "Test Product", "viewer@example.com", null, Map.of())))
                .isInstanceOf(ForbiddenException.class);

        ApiPayloads.LicenseLifecycleResponse license = platformService.issueLicense(owner.id(),
                new ApiPayloads.IssueLicenseRequest(viewer.id(), customerOrg.id(), policy.id(), "Viewer",
                        "viewer@example.com", "Test Product", "viewer@example.com", null, Map.of()));
        assertThat(platformService.listOrganizationLicenses(viewer.id(), customerOrg.id()))
                .extracting(ApiPayloads.LicenseLifecycleResponse::key)
                .contains(license.key());
    }

    @Test
    void shouldIssueActivateValidateCheckoutOfflineAndRevokeLicense() {
        ApiPayloads.PolicyResponse policy = createPolicy(LicensingModel.NODE_LOCKED, 1, 1);
        ApiPayloads.LicenseLifecycleResponse license = issueLicense(policy.id());

        ApiPayloads.ValidationResponse beforeActivation = platformService.validate(
                new ApiPayloads.ValidationRequest(license.key(), "test-product", policy.code(), "device-a", "1.2.0"));
        assertThat(beforeActivation.valid()).isTrue();
        assertThat(beforeActivation.code()).isEqualTo("MACHINE_NOT_ACTIVATED");

        ApiPayloads.MachineResponse machine = platformService.activate(license.key(),
                new ApiPayloads.ActivationRequest("device-a", "Developer Laptop", "macOS", "1.2.0"));
        assertThat(machine.status()).isEqualTo(MachineStatus.ACTIVE);

        ApiPayloads.ValidationResponse validation = platformService.validate(
                new ApiPayloads.ValidationRequest(license.key(), "test-product", policy.code(), "device-a", "1.2.0"));
        assertThat(validation.valid()).isTrue();
        assertThat(validation.entitlements()).containsExactly("feature.alpha");
        assertThat(validation.nextHeartbeatDueAt()).isNotNull();

        ApiPayloads.OfflineLicenseResponse offline = platformService.checkoutOffline(
                new ApiPayloads.OfflineCheckoutRequest(license.key(), "device-a", 2));
        assertThat(offline.artifact()).contains(".");

        ApiPayloads.OfflineVerifyResponse offlineValidation = platformService.verifyOffline(
                new ApiPayloads.OfflineVerifyRequest(offline.artifact()));
        assertThat(offlineValidation.valid()).isTrue();
        assertThat(offlineValidation.licenseKey()).isEqualTo(license.key());

        platformService.changeLicenseStatus(license.id(), LicenseStatus.REVOKED);
        ApiPayloads.ValidationResponse revoked = platformService.validate(
                new ApiPayloads.ValidationRequest(license.key(), "test-product", policy.code(), "device-a", "1.2.0"));
        assertThat(revoked.valid()).isFalse();
        assertThat(revoked.code()).isEqualTo("LICENSE_REVOKED");
    }

    @Test
    void shouldEnforceFloatingSeatsAndReclaimMissedHeartbeatSeat() {
        ApiPayloads.PolicyResponse policy = createPolicy(LicensingModel.FLOATING, 2, 1);
        ApiPayloads.LicenseLifecycleResponse license = issueLicense(policy.id());

        platformService.activate(license.key(), new ApiPayloads.ActivationRequest("device-a", null, null, "1.0.0"));
        assertThatThrownBy(() -> platformService.activate(license.key(),
                new ApiPayloads.ActivationRequest("device-b", null, null, "1.0.0")))
                .isInstanceOf(ConflictException.class);

        Machine machine = machineRepository.findByLicenseId(license.id()).getFirst();
        machine.setLastHeartbeatAt(LocalDateTime.now().minusMinutes(10));
        machineRepository.save(machine);

        platformService.activate(license.key(), new ApiPayloads.ActivationRequest("device-b", null, null, "1.0.0"));
        assertThat(machineRepository.findByLicenseId(license.id()))
                .extracting(Machine::getStatus)
                .contains(MachineStatus.HEARTBEAT_MISSED, MachineStatus.ACTIVE);
    }

    @Test
    void shouldRejectVersionOutsidePolicyBounds() {
        ApiPayloads.PolicyResponse policy = createPolicy(LicensingModel.NODE_LOCKED, 1, 1);
        ApiPayloads.LicenseLifecycleResponse license = issueLicense(policy.id());

        ApiPayloads.ValidationResponse validation = platformService.validate(
                new ApiPayloads.ValidationRequest(license.key(), "test-product", policy.code(), null, "0.9.0"));

        assertThat(validation.valid()).isFalse();
        assertThat(validation.code()).isEqualTo("VERSION_NOT_ALLOWED");
    }

    private ApiPayloads.PolicyResponse createPolicy(LicensingModel model, int maxMachines, int maxSeats) {
        ApiPayloads.ProductResponse product = platformService.createProduct(new ApiPayloads.CreateProductRequest(
                "test-product", "Test Product", "Test product", Map.of()));
        platformService.createEntitlement(product.id(), new ApiPayloads.CreateEntitlementRequest(
                "feature.alpha", "Alpha", "Alpha feature"));
        return platformService.createPolicy(new ApiPayloads.CreatePolicyRequest(
                product.id(), "test-policy-" + model.name().toLowerCase(), "Test Policy", model,
                maxMachines, maxSeats, 30, 1, 1, 5, "1.0.0", "2.0.0", Set.of("feature.alpha")));
    }

    private ApiPayloads.LicenseLifecycleResponse issueLicense(Long policyId) {
        return platformService.issueLicense(new ApiPayloads.IssueLicenseRequest(
                user.getId(), organization.getId(), policyId, "Customer", "customer@example.com",
                "Test Product", "customer@example.com", null, Map.of("plan", "test")));
    }
}
