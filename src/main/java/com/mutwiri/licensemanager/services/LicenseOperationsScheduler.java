/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.Machine;
import com.mutwiri.licensemanager.entities.MachineStatus;
import com.mutwiri.licensemanager.entities.SubscriptionStatus;
import com.mutwiri.licensemanager.repository.BillingSubscriptionRepository;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.MachineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class LicenseOperationsScheduler {
    private final LicenseRepository licenseRepository;
    private final MachineRepository machineRepository;
    private final BillingSubscriptionRepository subscriptionRepository;
    private final AuditService auditService;
    private final boolean enabled;
    private final int staleMachineDays;

    public LicenseOperationsScheduler(LicenseRepository licenseRepository,
            MachineRepository machineRepository,
            BillingSubscriptionRepository subscriptionRepository,
            AuditService auditService,
            @Value("${license.jobs.enabled:true}") boolean enabled,
            @Value("${license.jobs.stale-machine-days:30}") int staleMachineDays) {
        this.licenseRepository = licenseRepository;
        this.machineRepository = machineRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.auditService = auditService;
        this.enabled = enabled;
        this.staleMachineDays = staleMachineDays;
    }

    @Scheduled(fixedDelayString = "${license.jobs.expiry-sweep-delay-ms:300000}")
    @Transactional
    public void expireLicenses() {
        if (!enabled) {
            return;
        }
        int expired = 0;
        for (License license : licenseRepository.findByStatusAndActiveTrueAndExpiryBefore(
                LicenseStatus.ACTIVE, LocalDateTime.now())) {
            license.setStatus(LicenseStatus.EXPIRED);
            license.setActive(false);
            expired++;
            auditService.record("license.expired", "scheduler", "license", license.getId().toString(),
                    "License expired by scheduled sweep", Map.of("key", license.getKey()));
        }
        if (expired > 0) {
            log.info("Expired {} licenses during scheduled sweep", expired);
        }
    }

    @Scheduled(fixedDelayString = "${license.jobs.heartbeat-cleanup-delay-ms:300000}")
    @Transactional
    public void markMissedHeartbeats() {
        if (!enabled) {
            return;
        }
        int marked = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Machine machine : machineRepository.findByStatusAndLastHeartbeatAtBefore(MachineStatus.ACTIVE, now)) {
            if (machine.getLicense() == null || machine.getLicense().getPolicy() == null) {
                continue;
            }
            LocalDateTime graceCutoff = now.minusMinutes(machine.getLicense().getPolicy().getHeartbeatGracePeriodMinutes());
            LocalDateTime lastHeartbeat = machine.getLastHeartbeatAt();
            if (lastHeartbeat == null || !lastHeartbeat.isBefore(graceCutoff)) {
                continue;
            }
            machine.setStatus(MachineStatus.HEARTBEAT_MISSED);
            marked++;
            auditService.record("machine.heartbeat_missed", "scheduler", "machine", machine.getId().toString(),
                    "Machine heartbeat missed by scheduled cleanup", Map.of("license", machine.getLicense().getKey()));
        }
        if (marked > 0) {
            log.info("Marked {} machines as heartbeat missed", marked);
        }
    }

    @Scheduled(fixedDelayString = "${license.jobs.stale-machine-cleanup-delay-ms:3600000}")
    @Transactional
    public void deactivateStaleMissedMachines() {
        if (!enabled) {
            return;
        }
        int deactivated = 0;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(staleMachineDays);
        for (Machine machine : machineRepository.findByStatusAndUpdatedAtBefore(MachineStatus.HEARTBEAT_MISSED, cutoff)) {
            machine.setStatus(MachineStatus.DEACTIVATED);
            machine.setDeactivatedAt(LocalDateTime.now());
            deactivated++;
            auditService.record("machine.stale_deactivated", "scheduler", "machine", machine.getId().toString(),
                    "Stale missed-heartbeat machine deactivated", Map.of("fingerprintHash", machine.getFingerprintHash()));
        }
        if (deactivated > 0) {
            log.info("Deactivated {} stale missed-heartbeat machines", deactivated);
        }
    }

    @Scheduled(fixedDelayString = "${license.jobs.subscription-expiry-delay-ms:3600000}")
    @Transactional
    public void expireSubscriptions() {
        if (!enabled) {
            return;
        }
        int expired = 0;
        for (var subscription : subscriptionRepository.findByStatusAndCurrentPeriodEndBefore(
                SubscriptionStatus.ACTIVE, LocalDateTime.now())) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            expired++;
            auditService.record("billing.subscription_expired", "scheduler", "billing_subscription",
                    subscription.getId().toString(), "Billing subscription expired by scheduled sweep",
                    Map.of("organization", subscription.getOrganization().getDomain()));
        }
        if (expired > 0) {
            log.info("Expired {} billing subscriptions", expired);
        }
    }
}
