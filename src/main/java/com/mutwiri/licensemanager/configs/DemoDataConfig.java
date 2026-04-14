/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.configs;

import com.mutwiri.licensemanager.entities.LicensingModel;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.ProductRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

@Configuration
public class DemoDataConfig {
    @Bean
    @ConditionalOnProperty(name = "license.seed.enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner demoDataRunner(UserRepository userRepository,
            OrganizationRepository organizationRepository,
            ProductRepository productRepository,
            LicensePlatformService platformService) {
        return args -> {
            User user = userRepository.findByEmail("demo-admin@example.test").orElseGet(() -> {
                User demo = new User();
                demo.setName("Demo Admin");
                demo.setEmail("demo-admin@example.test");
                demo.setProvider("seed");
                demo.setProviderId("demo-admin");
                demo.setRole(UserRole.ADMIN);
                return userRepository.save(demo);
            });

            Organization organization = organizationRepository.findByDomain("demo.example.test").orElseGet(() -> {
                Organization demo = new Organization();
                demo.setName("Demo Customer");
                demo.setEmail("customer@example.test");
                demo.setDomain("demo.example.test");
                return organizationRepository.save(demo);
            });

            if (productRepository.existsByCode("demo-app")) {
                return;
            }

            ApiPayloads.ProductResponse product = platformService.createProduct(new ApiPayloads.CreateProductRequest(
                    "demo-app", "Demo App", "Seeded product for local licensing flows", Map.of("tier", "demo")));
            platformService.createEntitlement(product.id(), new ApiPayloads.CreateEntitlementRequest(
                    "feature.reports", "Reports", "Access to reporting features"));
            platformService.createEntitlement(product.id(), new ApiPayloads.CreateEntitlementRequest(
                    "feature.exports", "Exports", "Access to export features"));
            ApiPayloads.PolicyResponse policy = platformService.createPolicy(new ApiPayloads.CreatePolicyRequest(
                    product.id(), "demo-floating", "Demo Floating Plan", LicensingModel.FLOATING,
                    2, 2, 365, 60, 180, 7, "1.0.0", null,
                    Set.of("feature.reports", "feature.exports")));
            platformService.issueLicense(new ApiPayloads.IssueLicenseRequest(
                    user.getId(), organization.getId(), policy.id(), "Demo Customer",
                    "customer@example.test", "Demo App", "customer@example.test", null,
                    Map.of("seed", "true")));
        };
    }
}
