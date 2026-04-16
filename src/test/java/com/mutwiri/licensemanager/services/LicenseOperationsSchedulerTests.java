/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.BillingSubscription;
import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.Machine;
import com.mutwiri.licensemanager.entities.MachineStatus;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.Policy;
import com.mutwiri.licensemanager.entities.SubscriptionStatus;
import com.mutwiri.licensemanager.repository.BillingSubscriptionRepository;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.MachineRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LicenseOperationsSchedulerTests {

    private final LicenseRepository licenseRepository = mock(LicenseRepository.class);
    private final MachineRepository machineRepository = mock(MachineRepository.class);
    private final BillingSubscriptionRepository subscriptionRepository = mock(BillingSubscriptionRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final SchedulerLockService lockService = mock(SchedulerLockService.class);

    @Test
    void shouldSkipPublicScheduledJobsWhenDisabled() {
        LicenseOperationsScheduler scheduler = new LicenseOperationsScheduler(
                licenseRepository, machineRepository, subscriptionRepository, auditService, lockService, false, 30, 1);

        scheduler.expireLicenses();
        scheduler.markMissedHeartbeats();
        scheduler.deactivateStaleMissedMachines();
        scheduler.expireSubscriptions();

        verifyNoInteractions(lockService);
    }

    @Test
    void shouldExpireLicensesMarkHeartbeatsCleanupStaleMachinesAndExpireSubscriptions() {
        LicenseOperationsScheduler scheduler = new LicenseOperationsScheduler(
                licenseRepository, machineRepository, subscriptionRepository, auditService, lockService, true, 30, 1);
        License license = license();
        Machine activeMachine = machine(license, MachineStatus.ACTIVE, LocalDateTime.now().minusMinutes(10));
        Machine freshMachine = machine(license, MachineStatus.ACTIVE, LocalDateTime.now());
        Machine staleMachine = machine(license, MachineStatus.HEARTBEAT_MISSED, LocalDateTime.now().minusDays(40));
        BillingSubscription subscription = subscription();

        when(licenseRepository.findByStatusAndActiveTrueAndExpiryBefore(eq(LicenseStatus.ACTIVE), any()))
                .thenReturn(List.of(license));
        when(machineRepository.findByStatusAndLastHeartbeatAtBefore(eq(MachineStatus.ACTIVE), any()))
                .thenReturn(List.of(activeMachine, freshMachine, machine(null, MachineStatus.ACTIVE, LocalDateTime.now().minusMinutes(10))));
        when(machineRepository.findByStatusAndUpdatedAtBefore(eq(MachineStatus.HEARTBEAT_MISSED), any()))
                .thenReturn(List.of(staleMachine));
        when(subscriptionRepository.findByStatusAndCurrentPeriodEndBefore(eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(List.of(subscription));

        assertThat(scheduler.expireLicensesLocked()).isEqualTo(1);
        assertThat(scheduler.markMissedHeartbeatsLocked()).isEqualTo(1);
        assertThat(scheduler.deactivateStaleMissedMachinesLocked()).isEqualTo(1);
        assertThat(scheduler.expireSubscriptionsLocked()).isEqualTo(1);

        assertThat(license.getStatus()).isEqualTo(LicenseStatus.EXPIRED);
        assertThat(license.isActive()).isFalse();
        assertThat(activeMachine.getStatus()).isEqualTo(MachineStatus.HEARTBEAT_MISSED);
        assertThat(freshMachine.getStatus()).isEqualTo(MachineStatus.ACTIVE);
        assertThat(staleMachine.getStatus()).isEqualTo(MachineStatus.DEACTIVATED);
        assertThat(staleMachine.getDeactivatedAt()).isNotNull();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(auditService).record(eq("license.expired"), eq("scheduler"), eq("license"), eq("1"), any(), any(Map.class));
        verify(auditService).record(eq("machine.heartbeat_missed"), eq("scheduler"), eq("machine"), eq("2"), any(), any(Map.class));
        verify(auditService).record(eq("machine.stale_deactivated"), eq("scheduler"), eq("machine"), eq("4"), any(), any(Map.class));
        verify(auditService).record(eq("billing.subscription_expired"), eq("scheduler"), eq("billing_subscription"),
                eq("5"), any(), any(Map.class));
    }

    private License license() {
        Policy policy = new Policy();
        policy.setHeartbeatGracePeriodMinutes(1);
        License license = new License();
        license.setId(1L);
        license.setKey("lic_scheduler");
        license.setPolicy(policy);
        license.setStatus(LicenseStatus.ACTIVE);
        license.setActive(true);
        return license;
    }

    private Machine machine(License license, MachineStatus status, LocalDateTime lastHeartbeatAt) {
        Machine machine = new Machine();
        machine.setId(status == MachineStatus.HEARTBEAT_MISSED ? 4L : lastHeartbeatAt.isBefore(LocalDateTime.now().minusMinutes(1)) ? 2L : 3L);
        machine.setLicense(license);
        machine.setFingerprintHash("fp_" + machine.getId());
        machine.setStatus(status);
        machine.setLastHeartbeatAt(lastHeartbeatAt);
        return machine;
    }

    private BillingSubscription subscription() {
        Organization organization = new Organization();
        organization.setDomain("scheduler.example.com");
        BillingSubscription subscription = new BillingSubscription();
        subscription.setId(5L);
        subscription.setOrganization(organization);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }
}
