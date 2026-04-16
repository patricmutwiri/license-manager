/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.LicensingModel;
import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.Machine;
import com.mutwiri.licensemanager.entities.MachineStatus;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.OrganizationRole;
import com.mutwiri.licensemanager.entities.RuntimeTokenScope;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.exceptions.ConflictException;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.exceptions.InvalidLicenseRequestException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.MachineRepository;
import com.mutwiri.licensemanager.repository.OfflineLicenseArtifactRepository;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.ClientTokenService;
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

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private OfflineLicenseArtifactRepository artifactRepository;

    @Autowired
    private ClientTokenService clientTokenService;

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
        assertThatThrownBy(() -> platformService.createUser(new ApiPayloads.CreateUserRequest(
                "API Customer", "other-api-customer@example.com", UserRole.CUSTOMER, null, null)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> platformService.createOrganization(
                new ApiPayloads.CreateOrganizationRequest("Another Org", "other@example.com", "api-customer.example.com")))
                .isInstanceOf(ConflictException.class);
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
        ApiPayloads.ProductResponse ownedProduct = platformService.createProduct(owner.id(),
                new ApiPayloads.CreateProductRequest(customerOrg.id(), "rbac-product", "RBAC Product", "Owned product", Map.of()));
        platformService.createEntitlement(owner.id(), ownedProduct.id(),
                new ApiPayloads.CreateEntitlementRequest("feature.rbac", "RBAC Feature", "Feature"));
        ApiPayloads.PolicyResponse policy = platformService.createPolicy(owner.id(), new ApiPayloads.CreatePolicyRequest(
                ownedProduct.id(), "rbac-policy", "RBAC Policy", LicensingModel.NODE_LOCKED,
                1, 1, 30, 1, 1, 5, "1.0.0", "2.0.0", Set.of("feature.rbac")));

        assertThat(ownerMembership.permissions()).contains(com.mutwiri.licensemanager.entities.Permission.LICENSE_ISSUE);
        assertThat(ownedProduct.organizationId()).isEqualTo(customerOrg.id());
        assertThat(platformService.listProducts(owner.id())).extracting(ApiPayloads.ProductResponse::code)
                .contains("rbac-product");
        assertThat(platformService.createMembership(owner.id(),
                new ApiPayloads.CreateMembershipRequest(viewer.id(), customerOrg.id(), OrganizationRole.VIEWER))
                .role()).isEqualTo(OrganizationRole.VIEWER);
        assertThat(platformService.listMemberships(owner.id(), customerOrg.id()))
                .extracting(ApiPayloads.MembershipResponse::userEmail)
                .contains("viewer@example.com");
        assertThatThrownBy(() -> platformService.createProduct(viewer.id(),
                new ApiPayloads.CreateProductRequest(customerOrg.id(), "viewer-product", "Viewer Product", null, Map.of())))
                .isInstanceOf(ForbiddenException.class);
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
        assertThat(platformService.listProducts(viewer.id())).isEmpty();
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
        artifactRepository.findAll().forEach(artifact -> artifact.setRevoked(true));
        assertThat(platformService.verifyOffline(new ApiPayloads.OfflineVerifyRequest(offline.artifact())).code())
                .isEqualTo("OFFLINE_REVOKED");
        assertThat(platformService.verifyOffline(new ApiPayloads.OfflineVerifyRequest("not-a-token")).code())
                .isEqualTo("MALFORMED_ARTIFACT");
        assertThat(platformService.verifyOffline(new ApiPayloads.OfflineVerifyRequest(offline.artifact() + "tampered")).code())
                .isIn("BAD_SIGNATURE", "INVALID_ARTIFACT");

        platformService.changeLicenseStatus(license.id(), LicenseStatus.REVOKED);
        ApiPayloads.ValidationResponse revoked = platformService.validate(
                new ApiPayloads.ValidationRequest(license.key(), "test-product", policy.code(), "device-a", "1.2.0"));
        assertThat(revoked.valid()).isFalse();
        assertThat(revoked.code()).isEqualTo("LICENSE_REVOKED");
    }

    @Test
    void shouldRefreshHeartbeatDeactivateListAndRejectOfflineCheckoutForInactiveMachines() {
        ApiPayloads.PolicyResponse policy = createPolicy(LicensingModel.NODE_LOCKED, 1, 1);
        ApiPayloads.LicenseLifecycleResponse license = issueLicense(policy.id());

        ApiPayloads.MachineResponse activated = platformService.activate(license.key(),
                new ApiPayloads.ActivationRequest("device-lifecycle", "Laptop", "Linux", "1.0.0"));
        ApiPayloads.MachineResponse refreshed = platformService.activate(license.key(),
                new ApiPayloads.ActivationRequest("device-lifecycle", "Laptop", "Linux", "1.0.1"));
        ApiPayloads.MachineResponse heartbeat = platformService.heartbeat(license.key(),
                new ApiPayloads.HeartbeatRequest("device-lifecycle", "1.0.2"));

        assertThat(refreshed.id()).isEqualTo(activated.id());
        assertThat(heartbeat.version()).isEqualTo("1.0.2");
        assertThat(platformService.listMachines(license.id())).extracting(ApiPayloads.MachineResponse::id)
                .contains(activated.id());

        ApiPayloads.MachineResponse deactivated = platformService.deactivate(license.key(), "device-lifecycle");
        assertThat(deactivated.status()).isEqualTo(MachineStatus.DEACTIVATED);
        assertThatThrownBy(() -> platformService.checkoutOffline(
                new ApiPayloads.OfflineCheckoutRequest(license.key(), "device-lifecycle", 1)))
                .isInstanceOf(InvalidLicenseRequestException.class)
                .hasMessage("Machine must be active before offline checkout.");
        assertThatThrownBy(() -> platformService.heartbeat(license.key(),
                new ApiPayloads.HeartbeatRequest("missing-device", "1.0.0")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> platformService.deactivate(license.key(), "missing-device"))
                .isInstanceOf(ResourceNotFoundException.class);
        platformService.changeLicenseStatus(license.id(), LicenseStatus.SUSPENDED);
        assertThatThrownBy(() -> platformService.heartbeat(license.key(),
                new ApiPayloads.HeartbeatRequest("device-lifecycle", "1.0.3")))
                .isInstanceOf(InvalidLicenseRequestException.class);
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

    @Test
    void shouldRejectInvalidValidationScopesAndMissingResources() {
        ApiPayloads.PolicyResponse policy = createPolicy(LicensingModel.NODE_LOCKED, 1, 1);
        ApiPayloads.LicenseLifecycleResponse license = issueLicense(policy.id());

        assertThat(platformService.validate(new ApiPayloads.ValidationRequest(
                "lic_missing", "test-product", policy.code(), null, "1.0.0")).code())
                .isEqualTo("LICENSE_NOT_FOUND");
        assertThat(platformService.validate(new ApiPayloads.ValidationRequest(
                license.key(), "other-product", policy.code(), null, "1.0.0")).code())
                .isEqualTo("PRODUCT_MISMATCH");
        assertThat(platformService.validate(new ApiPayloads.ValidationRequest(
                license.key(), "test-product", "other-policy", null, "1.0.0")).code())
                .isEqualTo("POLICY_MISMATCH");
        assertThatThrownBy(() -> platformService.activate(license.key(),
                new ApiPayloads.ActivationRequest(" ", "Laptop", "Linux", "1.0.0")))
                .isInstanceOf(InvalidLicenseRequestException.class);
        assertThatThrownBy(() -> platformService.listMachines(404L))
                .isInstanceOf(ResourceNotFoundException.class);

        var expiredLicense = licenseRepository.findById(license.id()).orElseThrow();
        expiredLicense.setExpiry(LocalDateTime.now().minusDays(1));
        licenseRepository.save(expiredLicense);
        assertThat(platformService.validate(new ApiPayloads.ValidationRequest(
                license.key(), "test-product", policy.code(), null, "1.0.0")).code())
                .isEqualTo("LICENSE_EXPIRED");
    }

    @Test
    void shouldAuthorizeRuntimeTokenLifecycleAndListOperationalResources() {
        ApiPayloads.ProductResponse product = platformService.createProduct(new ApiPayloads.CreateProductRequest(
                null, "ops-product", "Ops Product", "Ops product", Map.of()));
        ApiPayloads.PolicyResponse policy = platformService.createPolicy(new ApiPayloads.CreatePolicyRequest(
                product.id(), "ops-policy", "Ops Policy", LicensingModel.NODE_LOCKED,
                1, 1, 30, 1, 1, 5, null, null, Set.of()));
        ApiPayloads.LicenseLifecycleResponse license = issueLicense(policy.id());
        ApiPayloads.ClientTokenResponse token = clientTokenService.create(new ApiPayloads.CreateClientTokenRequest(
                "ops-token", product.id(), license.id(), Set.of(RuntimeTokenScope.LICENSE_VALIDATE), null));

        platformService.authorizeClientTokenCreation(null, new ApiPayloads.CreateClientTokenRequest(
                "global", null, null, null, null));
        platformService.authorizeClientTokenCreation(null, new ApiPayloads.CreateClientTokenRequest(
                "product", product.id(), null, null, null));
        platformService.authorizeClientTokenLifecycle(null, token.id());

        assertThat(platformService.listPolicies()).extracting(ApiPayloads.PolicyResponse::code).contains("ops-policy");
        assertThat(platformService.listLicenses()).extracting(ApiPayloads.LicenseLifecycleResponse::key).contains(license.key());
        assertThat(platformService.recentAuditEvents()).isNotEmpty();
        assertThatThrownBy(() -> platformService.authorizeClientTokenLifecycle(null, 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRejectMissingResourcesAndDuplicateOperationalObjects() {
        ApiPayloads.PolicyResponse policy = createPolicy(LicensingModel.NODE_LOCKED, 1, 1);
        ApiPayloads.LicenseLifecycleResponse license = issueLicense(policy.id());

        assertThatThrownBy(() -> platformService.createProduct(new ApiPayloads.CreateProductRequest(
                404L, "missing-org-product", "Missing Org Product", null, Map.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> platformService.createProduct(new ApiPayloads.CreateProductRequest(
                null, "test-product", "Duplicate", null, Map.of())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> platformService.createEntitlement(404L,
                new ApiPayloads.CreateEntitlementRequest("missing", "Missing", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> platformService.createEntitlement(policy.productId(),
                new ApiPayloads.CreateEntitlementRequest("feature.alpha", "Duplicate", null)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> platformService.createPolicy(new ApiPayloads.CreatePolicyRequest(
                policy.productId(), policy.code(), "Duplicate", LicensingModel.NODE_LOCKED,
                1, 1, 30, 1, 1, 5, null, null, Set.of())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> platformService.createPolicy(new ApiPayloads.CreatePolicyRequest(
                404L, "missing-product-policy", "Missing Product Policy", LicensingModel.NODE_LOCKED,
                1, 1, 30, 1, 1, 5, null, null, Set.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> platformService.issueLicense(new ApiPayloads.IssueLicenseRequest(
                404L, organization.getId(), policy.id(), "Missing", "missing@example.com",
                null, null, null, Map.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> platformService.issueLicense(new ApiPayloads.IssueLicenseRequest(
                user.getId(), 404L, policy.id(), "Missing", "missing@example.com",
                null, null, null, Map.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> platformService.issueLicense(new ApiPayloads.IssueLicenseRequest(
                user.getId(), organization.getId(), 404L, "Missing", "missing@example.com",
                null, null, null, Map.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> platformService.changeLicenseStatus(404L, LicenseStatus.SUSPENDED))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(platformService.changeLicenseStatus(license.id(), LicenseStatus.SUSPENDED).status())
                .isEqualTo(LicenseStatus.SUSPENDED);

        License legacyLicense = new License();
        legacyLicense.setKey("lic_legacy_no_policy");
        legacyLicense.setStatus(LicenseStatus.ACTIVE);
        legacyLicense.setActive(true);
        legacyLicense.setExpiry(LocalDateTime.now().plusDays(1));
        legacyLicense = licenseRepository.save(legacyLicense);
        assertThat(platformService.listLicenses()).extracting(ApiPayloads.LicenseLifecycleResponse::key)
                .contains("lic_legacy_no_policy");
        assertThatThrownBy(() -> platformService.activate("lic_legacy_no_policy",
                new ApiPayloads.ActivationRequest("legacy-device", null, null, null)))
                .isInstanceOf(InvalidLicenseRequestException.class)
                .hasMessage("License has no policy and cannot use platform activation flows.");
        assertThat(platformService.validate(new ApiPayloads.ValidationRequest(
                "lic_legacy_no_policy", null, null, " ", null)).valid()).isTrue();
        assertThatThrownBy(() -> platformService.checkoutOffline(
                new ApiPayloads.OfflineCheckoutRequest("lic_missing", "device", 1)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> platformService.checkoutOffline(
                new ApiPayloads.OfflineCheckoutRequest(license.key(), "not-active", 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ApiPayloads.PolicyResponse createPolicy(LicensingModel model, int maxMachines, int maxSeats) {
        ApiPayloads.ProductResponse product = platformService.createProduct(new ApiPayloads.CreateProductRequest(
                null, "test-product", "Test Product", "Test product", Map.of()));
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
