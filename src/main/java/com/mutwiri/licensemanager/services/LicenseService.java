package com.mutwiri.licensemanager.services;

import java.util.List;
import java.util.Optional;

import com.mutwiri.licensemanager.entities.License;

public interface LicenseService {
    License generateLicense(Long userId, Long organizationId);

    boolean validateLicense(String key);

    List<License> getLicensesByUser(Long userId);

    List<License> getLicensesByOrganization(Long organizationId);

    Optional<License> getLicenseByKey(String key);
}
