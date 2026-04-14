/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 12:40 AM
 *
 */

package com.mutwiri.licensemanager.repository;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {
        Optional<License> findByKey(String key);

        List<License> findByUserId(Long userId);

        List<License> findByOrganizationId(Long organizationId);

        List<License> findByProductId(Long productId);

        List<License> findByPolicyId(Long policyId);

        List<License> findByStatus(LicenseStatus status);

        List<License> findByStatusAndActiveTrueAndExpiryBefore(LicenseStatus status, LocalDateTime expiry);
}
