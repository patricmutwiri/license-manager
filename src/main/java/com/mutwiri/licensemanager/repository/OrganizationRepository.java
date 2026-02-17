/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 12:36 AM
 *
 */

package com.mutwiri.licensemanager.repository;

import com.mutwiri.licensemanager.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByDomain(String domain);
    Organization findByName(String name);
    Organization findByEmail(String email);
    Optional<Organization> findById(Long id);
}
