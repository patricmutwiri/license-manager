/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.models.dto;

import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.LicensingModel;
import com.mutwiri.licensemanager.entities.MachineStatus;
import com.mutwiri.licensemanager.entities.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ApiPayloads {
    private ApiPayloads() {
    }

    public record CreateUserRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            UserRole role,
            String provider,
            String providerId) {
    }

    public record UserResponse(
            Long id,
            String name,
            String email,
            UserRole role,
            String provider,
            String providerId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record CreateOrganizationRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String domain) {
    }

    public record OrganizationResponse(
            Long id,
            String name,
            String email,
            String domain,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record CreateProductRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            Map<String, String> metadata) {
    }

    public record ProductResponse(
            Long id,
            String code,
            String name,
            String description,
            Map<String, String> metadata,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record CreateEntitlementRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description) {
    }

    public record EntitlementResponse(
            Long id,
            String code,
            String name,
            String description) {
    }

    public record CreatePolicyRequest(
            @NotNull Long productId,
            @NotBlank String code,
            @NotBlank String name,
            @NotNull LicensingModel licensingModel,
            @Min(1) int maxMachines,
            @Min(1) int maxSeats,
            @Min(1) int validityDays,
            @Min(1) int heartbeatIntervalMinutes,
            @Min(1) int heartbeatGracePeriodMinutes,
            @Min(1) int offlineTtlDays,
            String minVersion,
            String maxVersion,
            Set<String> entitlementCodes) {
    }

    public record PolicyResponse(
            Long id,
            Long productId,
            String productCode,
            String code,
            String name,
            LicensingModel licensingModel,
            int maxMachines,
            int maxSeats,
            int validityDays,
            int heartbeatIntervalMinutes,
            int heartbeatGracePeriodMinutes,
            int offlineTtlDays,
            String minVersion,
            String maxVersion,
            Set<String> entitlementCodes) {
    }

    public record IssueLicenseRequest(
            @NotNull Long userId,
            @NotNull Long organizationId,
            @NotNull Long policyId,
            String customerName,
            @Email String customerEmail,
            String applicationName,
            String email,
            LocalDateTime expiry,
            Map<String, String> metadata) {
    }

    public record ChangeLicenseStatusRequest(@NotNull LicenseStatus status) {
    }

    public record LicenseLifecycleResponse(
            Long id,
            String key,
            LicenseStatus status,
            String productCode,
            String policyCode,
            LocalDateTime expiry,
            String customerName,
            String customerEmail,
            Map<String, String> metadata) {
    }

    public record ValidationRequest(
            @NotBlank String key,
            String productCode,
            String policyCode,
            String fingerprint,
            String version) {
    }

    public record ValidationResponse(
            boolean valid,
            String code,
            String detail,
            String licenseKey,
            LicenseStatus licenseStatus,
            String productCode,
            String policyCode,
            List<String> entitlements,
            MachineResponse machine,
            LocalDateTime expiresAt,
            LocalDateTime nextHeartbeatDueAt) {
    }

    public record ActivationRequest(
            @NotBlank String fingerprint,
            String name,
            String platform,
            String version) {
    }

    public record MachineResponse(
            Long id,
            String fingerprintHash,
            String name,
            String platform,
            String version,
            MachineStatus status,
            LocalDateTime lastSeenAt,
            LocalDateTime lastHeartbeatAt) {
    }

    public record HeartbeatRequest(
            @NotBlank String fingerprint,
            String version) {
    }

    public record OfflineCheckoutRequest(
            @NotBlank String key,
            @NotBlank String fingerprint,
            Integer ttlDays) {
    }

    public record OfflineLicenseResponse(
            String artifact,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt) {
    }

    public record OfflineVerifyRequest(@NotBlank String artifact) {
    }

    public record OfflineVerifyResponse(
            boolean valid,
            String code,
            String detail,
            String licenseKey,
            String fingerprintHash,
            LocalDateTime expiresAt) {
    }

    public record OfflinePublicKeyResponse(String algorithm, String publicKeyBase64) {
    }

    public record AuditEventResponse(
            Long id,
            String eventType,
            String actor,
            String resourceType,
            String resourceId,
            String description,
            Map<String, String> metadata,
            LocalDateTime createdAt) {
    }

    public record CreateClientTokenRequest(
            @NotBlank String name,
            Long productId,
            Long licenseId,
            LocalDateTime expiresAt) {
    }

    public record ClientTokenResponse(
            Long id,
            String name,
            String tokenPrefix,
            String token,
            boolean active,
            LocalDateTime expiresAt) {
    }
}
