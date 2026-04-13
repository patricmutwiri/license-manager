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

/**
 * DTO for Organization responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {
    private Long id;
    private String name;
    private String email;
    private String domain;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

