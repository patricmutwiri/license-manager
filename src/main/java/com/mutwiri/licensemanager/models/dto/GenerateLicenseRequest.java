/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/24/26, 9:32 PM
 *
 */

package com.mutwiri.licensemanager.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for creating/updating licenses with validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateLicenseRequest {
    @NotBlank(message = "Organization ID is required")
    private Long organizationId;

    @NotBlank(message = "Application name is required")
    @Size(min = 1, max = 255, message = "Application name must be 1-255 characters")
    private String applicationName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(max = 255, message = "Hostname must not exceed 255 characters")
    private String hostname;

    private LocalDateTime expiryDate;

    private Map<String, String> customFields;
}

