/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/24/26, 9:32 PM
 *
 */

package com.mutwiri.licensemanager.models.dto;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.Organization;
import org.springframework.stereotype.Component;

/**
 * Maps between entities and DTOs.
 */
@Component
public class DtoMapper {

    public LicenseResponse toLicenseResponse(License license) {
        if (license == null) return null;

        return LicenseResponse.builder()
                .id(license.getId())
                .key(license.getKey())
                .applicationName(license.getApplicationName())
                .hostname(license.getHostname())
                .email(license.getEmail())
                .expiryDate(license.getExpiry())
                .active(license.isActive() && (license.getExpiry() == null || license.getExpiry().isAfter(java.time.LocalDateTime.now())))
                .customFields(license.getCustomFields())
                .createdAt(license.getCreatedAt())
                .updatedAt(license.getUpdatedAt())
                .build();
    }

    public OrganizationResponse toOrganizationResponse(Organization organization) {
        if (organization == null) return null;

        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .email(organization.getEmail())
                .domain(organization.getDomain())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }
}

