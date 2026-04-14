/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.repository;

import com.mutwiri.licensemanager.entities.OrganizationMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    List<OrganizationMembership> findByOrganizationId(Long organizationId);

    List<OrganizationMembership> findByUserId(Long userId);

    Optional<OrganizationMembership> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);
}
