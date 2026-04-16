/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager.configs;

import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.OrganizationRole;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.ProductRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoDataConfigTests {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final LicensePlatformService platformService = mock(LicensePlatformService.class);
    private final DemoDataConfig config = new DemoDataConfig();

    @Test
    void shouldSeedDemoDataWhenProductDoesNotExist() throws Exception {
        User user = demoUser();
        Organization organization = demoOrganization();
        ApiPayloads.ProductResponse product = new ApiPayloads.ProductResponse(
                10L, 20L, "demo.example.test", "demo-app", "Demo App", "Demo", null, null, null);
        ApiPayloads.PolicyResponse policy = new ApiPayloads.PolicyResponse(
                30L, 10L, "demo-app", "demo-floating", "Demo Floating Plan",
                com.mutwiri.licensemanager.entities.LicensingModel.FLOATING, 2, 2, 365,
                60, 180, 7, "1.0.0", null, Set.of("feature.reports", "feature.exports"));

        when(userRepository.findByEmail("demo-admin@example.test")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(organizationRepository.findByDomain("demo.example.test")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);
        when(productRepository.existsByCode("demo-app")).thenReturn(false);
        when(platformService.createProduct(any())).thenReturn(product);
        when(platformService.createPolicy(any())).thenReturn(policy);

        runner().run(null);

        verify(platformService).createMembership(null,
                new ApiPayloads.CreateMembershipRequest(1L, 2L, OrganizationRole.OWNER));
        verify(platformService).createProduct(any());
        verify(platformService).createEntitlement(10L,
                new ApiPayloads.CreateEntitlementRequest("feature.reports", "Reports", "Access to reporting features"));
        verify(platformService).createEntitlement(10L,
                new ApiPayloads.CreateEntitlementRequest("feature.exports", "Exports", "Access to export features"));
        verify(platformService).createPolicy(any());
        verify(platformService).issueLicense(any());
    }

    @Test
    void shouldOnlyEnsureMembershipWhenDemoProductAlreadyExists() throws Exception {
        when(userRepository.findByEmail("demo-admin@example.test")).thenReturn(Optional.of(demoUser()));
        when(organizationRepository.findByDomain("demo.example.test")).thenReturn(Optional.of(demoOrganization()));
        when(productRepository.existsByCode("demo-app")).thenReturn(true);

        runner().run(null);

        verify(platformService).createMembership(null,
                new ApiPayloads.CreateMembershipRequest(1L, 2L, OrganizationRole.OWNER));
        verify(platformService, never()).createProduct(any());
        verify(platformService, never()).issueLicense(any());
    }

    private ApplicationRunner runner() {
        return config.demoDataRunner(userRepository, organizationRepository, productRepository, platformService);
    }

    private User demoUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Demo Admin");
        user.setEmail("demo-admin@example.test");
        user.setProvider("seed");
        user.setProviderId("demo-admin");
        user.setRole(UserRole.ADMIN);
        return user;
    }

    private Organization demoOrganization() {
        Organization organization = new Organization();
        organization.setId(2L);
        organization.setName("Demo Customer");
        organization.setEmail("customer@example.test");
        organization.setDomain("demo.example.test");
        return organization;
    }
}
