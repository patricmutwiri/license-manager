/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager.repository;

import com.mutwiri.licensemanager.entities.SchedulerLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchedulerLockRepository extends JpaRepository<SchedulerLock, String> {
}
