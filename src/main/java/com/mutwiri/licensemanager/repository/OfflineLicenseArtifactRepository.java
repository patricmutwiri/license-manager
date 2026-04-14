/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.repository;

import com.mutwiri.licensemanager.entities.OfflineLicenseArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfflineLicenseArtifactRepository extends JpaRepository<OfflineLicenseArtifact, Long> {
    Optional<OfflineLicenseArtifact> findByTokenHash(String tokenHash);
}

