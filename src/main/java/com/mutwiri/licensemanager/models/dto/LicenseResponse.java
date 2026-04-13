/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/24/26, 9:32 PM
 *
 */

package com.mutwiri.licensemanager.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for License responses. Does not expose sensitive internal fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseResponse {
    private Long id;
    private String key;
    private String applicationName;
    private String hostname;
    private String email;
    private LocalDateTime expiryDate;
    private boolean active;
    private Map<String, String> customFields;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isExpiringSoon() {
        if (expiryDate == null) return false;
        return expiryDate.minusDays(30).isBefore(LocalDateTime.now());
    }

    public long daysUntilExpiry() {
        if (expiryDate == null) return -1;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);
    }
}

