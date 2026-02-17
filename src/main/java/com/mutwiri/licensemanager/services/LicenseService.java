package com.mutwiri.licensemanager.services;

import java.util.List;
import java.util.Optional;

import com.mutwiri.licensemanager.entities.License;

public interface LicenseService {
    License generateLicense(Long userId, Long organizationId);

    License generateLicense(Long userId, Long organizationId, String hostname, String applicationName, String email,
            java.time.LocalDateTime expiryDate, java.util.Map<String, String> customFields);

    boolean validateLicense(String key);

    List<License> getLicensesByUser(Long userId);

    List<License> getLicensesByOrganization(Long organizationId);

    Optional<License> getLicenseByKey(String key);
}
