/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.controllers.PlatformAdminController;
import com.mutwiri.licensemanager.entities.BillingInterval;
import com.mutwiri.licensemanager.entities.BillingProvider;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.LicensingModel;
import com.mutwiri.licensemanager.entities.OrganizationRole;
import com.mutwiri.licensemanager.entities.Permission;
import com.mutwiri.licensemanager.entities.RuntimeTokenScope;
import com.mutwiri.licensemanager.entities.SubscriptionStatus;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.services.AdminAuthService;
import com.mutwiri.licensemanager.services.BillingService;
import com.mutwiri.licensemanager.services.ClientTokenService;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformAdminControllerUnitTests {
    private final LicensePlatformService platformService = mock(LicensePlatformService.class);
    private final AdminAuthService adminAuthService = mock(AdminAuthService.class);
    private final ClientTokenService clientTokenService = mock(ClientTokenService.class);
    private final BillingService billingService = mock(BillingService.class);
    private final PlatformAdminController controller = new PlatformAdminController(
            platformService, adminAuthService, clientTokenService, billingService);

    @Test
    void shouldRouteEveryAdminWorkflowThroughAuthorizationAndServices() {
        LocalDateTime now = LocalDateTime.now();
        ApiPayloads.CreateUserRequest createUser = new ApiPayloads.CreateUserRequest(
                "Admin User", "admin@example.com", UserRole.ADMIN, "github", "42");
        ApiPayloads.UserResponse user = new ApiPayloads.UserResponse(
                1L, "Admin User", "admin@example.com", UserRole.ADMIN, "github", "42", now, now);
        ApiPayloads.CreateOrganizationRequest createOrganization = new ApiPayloads.CreateOrganizationRequest(
                "Acme", "ops@example.com", "acme.example.com");
        ApiPayloads.OrganizationResponse organization = new ApiPayloads.OrganizationResponse(
                2L, "Acme", "ops@example.com", "acme.example.com", now, now);
        ApiPayloads.CreateMembershipRequest createMembership = new ApiPayloads.CreateMembershipRequest(
                1L, 999L, OrganizationRole.OWNER);
        ApiPayloads.CreateMembershipRequest scopedMembership = new ApiPayloads.CreateMembershipRequest(
                1L, 2L, OrganizationRole.OWNER);
        ApiPayloads.MembershipResponse membership = new ApiPayloads.MembershipResponse(
                3L, 1L, "admin@example.com", 2L, "acme.example.com", OrganizationRole.OWNER,
                Set.of(Permission.LICENSE_ISSUE), now, now);
        ApiPayloads.CreateProductRequest createProduct = new ApiPayloads.CreateProductRequest(
                2L, "prod", "Product", "Description", Map.of("tier", "enterprise"));
        ApiPayloads.ProductResponse product = new ApiPayloads.ProductResponse(
                4L, 2L, "acme.example.com", "prod", "Product", "Description", Map.of(), now, now);
        ApiPayloads.CreateEntitlementRequest createEntitlement = new ApiPayloads.CreateEntitlementRequest(
                "feature.audit", "Audit", "Audit log");
        ApiPayloads.EntitlementResponse entitlement = new ApiPayloads.EntitlementResponse(
                5L, "feature.audit", "Audit", "Audit log");
        ApiPayloads.CreatePolicyRequest createPolicy = new ApiPayloads.CreatePolicyRequest(
                4L, "policy", "Policy", LicensingModel.FLOATING, 5, 5, 365, 10, 2, 7,
                "1.0.0", "2.0.0", Set.of("feature.audit"));
        ApiPayloads.PolicyResponse policy = new ApiPayloads.PolicyResponse(
                6L, 4L, "prod", "policy", "Policy", LicensingModel.FLOATING, 5, 5, 365,
                10, 2, 7, "1.0.0", "2.0.0", Set.of("feature.audit"));
        ApiPayloads.IssueLicenseRequest issueLicense = new ApiPayloads.IssueLicenseRequest(
                1L, 2L, 6L, "Customer", "customer@example.com", "Product",
                "customer@example.com", now.plusDays(30), Map.of("plan", "enterprise"));
        ApiPayloads.LicenseLifecycleResponse license = new ApiPayloads.LicenseLifecycleResponse(
                7L, "lic-key", LicenseStatus.ACTIVE, "prod", "policy", now.plusDays(30),
                "Customer", "customer@example.com", Map.of());
        ApiPayloads.ChangeLicenseStatusRequest changeStatus = new ApiPayloads.ChangeLicenseStatusRequest(
                LicenseStatus.SUSPENDED);
        ApiPayloads.MachineResponse machine = new ApiPayloads.MachineResponse(
                8L, "fingerprint-hash", "Laptop", "macOS", "1.0.0",
                com.mutwiri.licensemanager.entities.MachineStatus.ACTIVE, now, now);
        ApiPayloads.AuditEventResponse event = new ApiPayloads.AuditEventResponse(
                9L, "license.issued", "admin", "license", "7", "Issued", Map.of(), now);
        ApiPayloads.CreateBillingPlanRequest createPlan = new ApiPayloads.CreateBillingPlanRequest(
                "enterprise", "Enterprise", 6L, 10000, "USD", BillingInterval.MONTHLY,
                14, BillingProvider.INTERNAL, null, Map.of());
        ApiPayloads.BillingPlanResponse plan = new ApiPayloads.BillingPlanResponse(
                10L, "enterprise", "Enterprise", 6L, "policy", 10000, "USD",
                BillingInterval.MONTHLY, 14, true, BillingProvider.INTERNAL, null, Map.of());
        ApiPayloads.CreateBillingSubscriptionRequest createSubscription =
                new ApiPayloads.CreateBillingSubscriptionRequest(2L, 10L, SubscriptionStatus.ACTIVE,
                        BillingProvider.INTERNAL, "cus_1", "sub_1", now, now.plusMonths(1), false);
        ApiPayloads.BillingSubscriptionResponse subscription = new ApiPayloads.BillingSubscriptionResponse(
                11L, 2L, "acme.example.com", 10L, "enterprise", SubscriptionStatus.ACTIVE,
                BillingProvider.INTERNAL, "cus_1", "sub_1", now, now.plusMonths(1), false);
        ApiPayloads.CreateClientTokenRequest createToken = new ApiPayloads.CreateClientTokenRequest(
                "runtime", 4L, null, Set.of(RuntimeTokenScope.LICENSE_VALIDATE), now.plusDays(1));
        ApiPayloads.RotateClientTokenRequest rotateToken = new ApiPayloads.RotateClientTokenRequest(
                Set.of(RuntimeTokenScope.MACHINE_ACTIVATE), now.plusDays(2));
        ApiPayloads.ClientTokenResponse token = new ApiPayloads.ClientTokenResponse(
                12L, "runtime", "lct_prefix", "secret", true,
                Set.of(RuntimeTokenScope.LICENSE_VALIDATE), now.plusDays(1));

        when(platformService.createUser(100L, createUser)).thenReturn(user);
        when(platformService.listUsers(100L)).thenReturn(List.of(user));
        when(platformService.createOrganization(100L, createOrganization)).thenReturn(organization);
        when(platformService.listOrganizations(100L)).thenReturn(List.of(organization));
        when(platformService.createMembership(100L, scopedMembership)).thenReturn(membership);
        when(platformService.listMemberships(100L, 2L)).thenReturn(List.of(membership));
        when(platformService.listOrganizationLicenses(100L, 2L)).thenReturn(List.of(license));
        when(platformService.createProduct(100L, createProduct)).thenReturn(product);
        when(platformService.listProducts(100L)).thenReturn(List.of(product));
        when(platformService.createEntitlement(100L, 4L, createEntitlement)).thenReturn(entitlement);
        when(platformService.createPolicy(100L, createPolicy)).thenReturn(policy);
        when(platformService.listPolicies(100L)).thenReturn(List.of(policy));
        when(platformService.issueLicense(100L, issueLicense)).thenReturn(license);
        when(platformService.listLicenses(100L)).thenReturn(List.of(license));
        when(platformService.changeLicenseStatus(100L, 7L, LicenseStatus.SUSPENDED)).thenReturn(license);
        when(platformService.listMachines(100L, 7L)).thenReturn(List.of(machine));
        when(platformService.recentAuditEvents(100L)).thenReturn(List.of(event));
        when(billingService.createPlan(100L, createPlan)).thenReturn(plan);
        when(billingService.listPlans(100L)).thenReturn(List.of(plan));
        when(billingService.createSubscription(100L, createSubscription)).thenReturn(subscription);
        when(billingService.listOrganizationSubscriptions(100L, 2L)).thenReturn(List.of(subscription));
        when(clientTokenService.create(createToken)).thenReturn(token);
        when(clientTokenService.rotate(12L, rotateToken)).thenReturn(token);
        when(clientTokenService.revoke(12L)).thenReturn(token);

        assertThat(controller.createUser("admin-key", 100L, createUser).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.listUsers("admin-key", 100L)).containsExactly(user);
        assertThat(controller.createOrganization("admin-key", 100L, createOrganization).getBody()).isEqualTo(organization);
        assertThat(controller.listOrganizations("admin-key", 100L)).containsExactly(organization);
        assertThat(controller.createMembership("admin-key", 100L, 2L, createMembership).getBody()).isEqualTo(membership);
        assertThat(controller.listMemberships("admin-key", 100L, 2L)).containsExactly(membership);
        assertThat(controller.listOrganizationLicenses("admin-key", 100L, 2L)).containsExactly(license);
        assertThat(controller.createProduct("admin-key", 100L, createProduct).getBody()).isEqualTo(product);
        assertThat(controller.listProducts("admin-key", 100L)).containsExactly(product);
        assertThat(controller.createEntitlement("admin-key", 100L, 4L, createEntitlement).getBody()).isEqualTo(entitlement);
        assertThat(controller.createPolicy("admin-key", 100L, createPolicy).getBody()).isEqualTo(policy);
        assertThat(controller.listPolicies("admin-key", 100L)).containsExactly(policy);
        assertThat(controller.issueLicense("admin-key", 100L, issueLicense).getBody()).isEqualTo(license);
        assertThat(controller.listLicenses("admin-key", 100L)).containsExactly(license);
        assertThat(controller.changeLicenseStatus("admin-key", 100L, 7L, changeStatus)).isEqualTo(license);
        assertThat(controller.listMachines("admin-key", 100L, 7L)).containsExactly(machine);
        assertThat(controller.auditEvents("admin-key", 100L)).containsExactly(event);
        assertThat(controller.createBillingPlan("admin-key", 100L, createPlan).getBody()).isEqualTo(plan);
        assertThat(controller.listBillingPlans("admin-key", 100L)).containsExactly(plan);
        assertThat(controller.createBillingSubscription("admin-key", 100L, createSubscription).getBody()).isEqualTo(subscription);
        assertThat(controller.listOrganizationBillingSubscriptions("admin-key", 100L, 2L)).containsExactly(subscription);
        assertThat(controller.createClientToken("admin-key", 100L, createToken).getBody()).isEqualTo(token);
        assertThat(controller.rotateClientToken("admin-key", 100L, 12L, rotateToken)).isEqualTo(token);
        assertThat(controller.revokeClientToken("admin-key", 100L, 12L)).isEqualTo(token);

        verify(adminAuthService, times(24)).requireAdmin("admin-key");
        verify(platformService).authorizeClientTokenCreation(100L, createToken);
        verify(platformService, times(2)).authorizeClientTokenLifecycle(100L, 12L);
    }

    @Test
    void shouldStopBeforeServiceCallWhenAdminKeyIsInvalid() {
        org.mockito.Mockito.doThrow(new ForbiddenException("denied")).when(adminAuthService).requireAdmin(null);

        assertThatThrownBy(() -> controller.listUsers(null, null)).isInstanceOf(ForbiddenException.class);
    }
}
