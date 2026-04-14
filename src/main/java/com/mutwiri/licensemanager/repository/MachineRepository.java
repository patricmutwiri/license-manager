/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.repository;

import com.mutwiri.licensemanager.entities.Machine;
import com.mutwiri.licensemanager.entities.MachineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
    Optional<Machine> findByLicenseIdAndFingerprintHash(Long licenseId, String fingerprintHash);

    List<Machine> findByLicenseId(Long licenseId);

    long countByLicenseIdAndStatus(Long licenseId, MachineStatus status);
}

