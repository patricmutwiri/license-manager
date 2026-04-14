/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.controllers;

import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.services.AdminAuthService;
import com.mutwiri.licensemanager.services.ClientTokenService;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class PlatformAdminController {
    private static final String ADMIN_KEY_HEADER = "X-Admin-Api-Key";
    private static final String ACTOR_HEADER = "X-Actor-User-Id";

    private final LicensePlatformService platformService;
    private final AdminAuthService adminAuthService;
    private final ClientTokenService clientTokenService;

    public PlatformAdminController(LicensePlatformService platformService,
            AdminAuthService adminAuthService,
            ClientTokenService clientTokenService) {
        this.platformService = platformService;
        this.adminAuthService = adminAuthService;
        this.clientTokenService = clientTokenService;
    }

    @PostMapping("/users")
    public ResponseEntity<ApiPayloads.UserResponse> createUser(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @Valid @RequestBody ApiPayloads.CreateUserRequest request) {
        adminAuthService.requireAdmin(apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createUser(actorUserId, request));
    }

    @GetMapping("/users")
    public List<ApiPayloads.UserResponse> listUsers(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId) {
        adminAuthService.requireAdmin(apiKey);
        return platformService.listUsers(actorUserId);
    }

    @PostMapping("/organizations")
    public ResponseEntity<ApiPayloads.OrganizationResponse> createOrganization(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @Valid @RequestBody ApiPayloads.CreateOrganizationRequest request) {
        adminAuthService.requireAdmin(apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createOrganization(actorUserId, request));
    }

    @GetMapping("/organizations")
    public List<ApiPayloads.OrganizationResponse> listOrganizations(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId) {
        adminAuthService.requireAdmin(apiKey);
        return platformService.listOrganizations(actorUserId);
    }

    @PostMapping("/organizations/{organizationId}/memberships")
    public ResponseEntity<ApiPayloads.MembershipResponse> createMembership(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @PathVariable Long organizationId,
            @Valid @RequestBody ApiPayloads.CreateMembershipRequest request) {
        adminAuthService.requireAdmin(apiKey);
        ApiPayloads.CreateMembershipRequest scopedRequest = new ApiPayloads.CreateMembershipRequest(
                request.userId(), organizationId, request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createMembership(actorUserId, scopedRequest));
    }

    @GetMapping("/organizations/{organizationId}/memberships")
    public List<ApiPayloads.MembershipResponse> listMemberships(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @PathVariable Long organizationId) {
        adminAuthService.requireAdmin(apiKey);
        return platformService.listMemberships(actorUserId, organizationId);
    }

    @GetMapping("/organizations/{organizationId}/licenses")
    public List<ApiPayloads.LicenseLifecycleResponse> listOrganizationLicenses(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @PathVariable Long organizationId) {
        adminAuthService.requireAdmin(apiKey);
        return platformService.listOrganizationLicenses(actorUserId, organizationId);
    }

    @PostMapping("/products")
    public ResponseEntity<ApiPayloads.ProductResponse> createProduct(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @Valid @RequestBody ApiPayloads.CreateProductRequest request) {
        adminAuthService.requireAdmin(apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createProduct(actorUserId, request));
    }

    @GetMapping("/products")
    public List<ApiPayloads.ProductResponse> listProducts(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId) {
        adminAuthService.requireAdmin(apiKey);
        return platformService.listProducts(actorUserId);
    }

    @PostMapping("/products/{productId}/entitlements")
    public ResponseEntity<ApiPayloads.EntitlementResponse> createEntitlement(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @PathVariable Long productId,
            @Valid @RequestBody ApiPayloads.CreateEntitlementRequest request) {
        adminAuthService.requireAdmin(apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createEntitlement(actorUserId, productId, request));
    }

    @PostMapping("/policies")
    public ResponseEntity<ApiPayloads.PolicyResponse> createPolicy(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @Valid @RequestBody ApiPayloads.CreatePolicyRequest request) {
        adminAuthService.requireAdmin(apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createPolicy(actorUserId, request));
    }

    @GetMapping("/policies")
    public List<ApiPayloads.PolicyResponse> listPolicies(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId) {
        adminAuthService.requireAdmin(apiKey);
        return platformService.listPolicies(actorUserId);
    }

    @PostMapping("/licenses")
    public ResponseEntity<ApiPayloads.LicenseLifecycleResponse> issueLicense(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @Valid @RequestBody ApiPayloads.IssueLicenseRequest request) {
        adminAuthService.requireAdmin(apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.issueLicense(actorUserId, request));
    }

    @GetMapping("/licenses")
    public List<ApiPayloads.LicenseLifecycleResponse> listLicenses(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId) {
        adminAuthService.requireAdmin(apiKey);
        return platformService.listLicenses(actorUserId);
    }

    @PatchMapping("/licenses/{licenseId}/status")
    public ApiPayloads.LicenseLifecycleResponse changeLicenseStatus(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @PathVariable Long licenseId,
            @Valid @RequestBody ApiPayloads.ChangeLicenseStatusRequest request) {
        adminAuthService.requireAdmin(apiKey);
        return platformService.changeLicenseStatus(actorUserId, licenseId, request.status());
    }

    @GetMapping("/licenses/{licenseId}/machines")
    public List<ApiPayloads.MachineResponse> listMachines(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @PathVariable Long licenseId) {
        adminAuthService.requireAdmin(apiKey);
        return platformService.listMachines(actorUserId, licenseId);
    }

    @GetMapping("/audit-events")
    public List<ApiPayloads.AuditEventResponse> auditEvents(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId) {
        adminAuthService.requireAdmin(apiKey);
        return platformService.recentAuditEvents(actorUserId);
    }

    @PostMapping("/client-tokens")
    public ResponseEntity<ApiPayloads.ClientTokenResponse> createClientToken(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) Long actorUserId,
            @Valid @RequestBody ApiPayloads.CreateClientTokenRequest request) {
        adminAuthService.requireAdmin(apiKey);
        platformService.authorizeClientTokenCreation(actorUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(clientTokenService.create(request));
    }
}
