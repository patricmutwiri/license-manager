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

/**
 * DTO for creating/updating organizations with validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrganizationRequest {
    @NotBlank(message = "Organization name is required")
    @Size(min = 1, max = 255, message = "Organization name must be 1-255 characters")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Domain is required")
    @Size(min = 1, max = 255, message = "Domain must be 1-255 characters")
    private String domain;
}

