/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.repository;

import com.mutwiri.licensemanager.entities.BillingSubscription;
import com.mutwiri.licensemanager.entities.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillingSubscriptionRepository extends JpaRepository<BillingSubscription, Long> {
    List<BillingSubscription> findByOrganizationId(Long organizationId);

    List<BillingSubscription> findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus status, LocalDateTime cutoff);
}
