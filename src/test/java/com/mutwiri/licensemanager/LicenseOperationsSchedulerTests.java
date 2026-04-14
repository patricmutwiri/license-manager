/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.LicensingModel;
import com.mutwiri.licensemanager.entities.Machine;
import com.mutwiri.licensemanager.entities.MachineStatus;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.MachineRepository;
import com.mutwiri.licensemanager.services.EmailService;
import com.mutwiri.licensemanager.services.LicenseOperationsScheduler;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LicenseOperationsSchedulerTests {
    @Autowired
    private LicenseOperationsScheduler scheduler;

    @Autowired
    private LicensePlatformService platformService;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private MachineRepository machineRepository;

    @MockitoBean
    private EmailService emailService;

    @Test
    void shouldExpireLicensesAndMarkMissedHeartbeats() {
        ApiPayloads.UserResponse user = platformService.createUser(new ApiPayloads.CreateUserRequest(
                "Scheduler User", "scheduler-user@example.com", null, null, null));
        ApiPayloads.OrganizationResponse organization = platformService.createOrganization(
                new ApiPayloads.CreateOrganizationRequest("Scheduler Org", "scheduler-org@example.com", "scheduler.example.com"));
        ApiPayloads.ProductResponse product = platformService.createProduct(new ApiPayloads.CreateProductRequest(
                organization.id(), "scheduler-product", "Scheduler Product", "Scheduler product", Map.of()));
        ApiPayloads.PolicyResponse policy = platformService.createPolicy(new ApiPayloads.CreatePolicyRequest(
                product.id(), "scheduler-policy", "Scheduler Policy", LicensingModel.NODE_LOCKED,
                1, 1, 1, 1, 1, 1, null, null, Set.of()));
        ApiPayloads.LicenseLifecycleResponse response = platformService.issueLicense(new ApiPayloads.IssueLicenseRequest(
                user.id(), organization.id(), policy.id(), "Scheduler", "scheduler@example.com",
                "Scheduler Product", "scheduler@example.com", LocalDateTime.now().plusDays(1), Map.of()));
        platformService.activate(response.key(), new ApiPayloads.ActivationRequest("scheduler-device", null, null, "1.0.0"));

        License license = licenseRepository.findById(response.id()).orElseThrow();
        license.setExpiry(LocalDateTime.now().minusDays(1));
        licenseRepository.save(license);

        Machine machine = machineRepository.findByLicenseId(response.id()).getFirst();
        machine.setLastHeartbeatAt(LocalDateTime.now().minusMinutes(10));
        machineRepository.save(machine);

        scheduler.expireLicenses();
        scheduler.markMissedHeartbeats();

        assertThat(licenseRepository.findById(license.getId()).orElseThrow().getStatus()).isEqualTo(LicenseStatus.EXPIRED);
        assertThat(machineRepository.findById(machine.getId()).orElseThrow().getStatus()).isEqualTo(MachineStatus.HEARTBEAT_MISSED);
    }
}
