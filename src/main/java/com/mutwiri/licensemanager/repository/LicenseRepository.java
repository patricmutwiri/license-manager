/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mutwiri.licensemanager.entities.License;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {
        Optional<License> findByKey(String key);

        List<License> findByUserId(Long userId);

        List<License> findByOrganizationId(Long organizationId);
}
