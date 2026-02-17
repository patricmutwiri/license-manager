/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 12:40 AM
 *
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.License;

import java.util.List;
import java.util.Optional;

public interface LicenseService {
    License generateLicense(Long userId, Long organizationId);

    License generateLicense(Long userId, Long organizationId, String hostname, String applicationName, String email,
            java.time.LocalDateTime expiryDate, java.util.Map<String, String> customFields);

    boolean validateLicense(String key);

    List<License> getLicensesByUser(Long userId);

    List<License> getLicensesByOrganization(Long organizationId);

    Optional<License> getLicenseByKey(String key);
}
