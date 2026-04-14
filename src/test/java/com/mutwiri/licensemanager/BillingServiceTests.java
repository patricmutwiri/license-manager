/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.entities.BillingInterval;
import com.mutwiri.licensemanager.entities.BillingProvider;
import com.mutwiri.licensemanager.entities.LicensingModel;
import com.mutwiri.licensemanager.entities.OrganizationRole;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.services.BillingService;
import com.mutwiri.licensemanager.services.EmailService;
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
class BillingServiceTests {
    @Autowired
    private BillingService billingService;

    @Autowired
    private LicensePlatformService platformService;

    @MockitoBean
    private EmailService emailService;

    @Test
    void shouldCreateProviderAgnosticPlansAndOrganizationSubscriptions() {
        ApiPayloads.UserResponse billingUser = platformService.createUser(new ApiPayloads.CreateUserRequest(
                "Billing User", "billing-user@example.com", UserRole.CUSTOMER, null, null));
        ApiPayloads.OrganizationResponse organization = platformService.createOrganization(
                new ApiPayloads.CreateOrganizationRequest("Billing Org", "billing-org@example.com", "billing.example.com"));
        platformService.createMembership(null,
                new ApiPayloads.CreateMembershipRequest(billingUser.id(), organization.id(), OrganizationRole.BILLING));
        ApiPayloads.ProductResponse product = platformService.createProduct(null, new ApiPayloads.CreateProductRequest(
                organization.id(), "billing-product", "Billing Product", "Billing product", Map.of()));
        ApiPayloads.PolicyResponse policy = platformService.createPolicy(null, new ApiPayloads.CreatePolicyRequest(
                product.id(), "billing-policy", "Billing Policy", LicensingModel.FLOATING,
                10, 10, 365, 60, 180, 7, null, null, Set.of()));

        ApiPayloads.BillingPlanResponse plan = billingService.createPlan(null, new ApiPayloads.CreateBillingPlanRequest(
                "team-monthly", "Team Monthly", policy.id(), 4900, "usd", BillingInterval.MONTHLY,
                14, BillingProvider.INTERNAL, null, Map.of("seats", "10")));
        ApiPayloads.BillingSubscriptionResponse subscription = billingService.createSubscription(billingUser.id(),
                new ApiPayloads.CreateBillingSubscriptionRequest(organization.id(), plan.id(), null, null,
                        "cus_internal", "sub_internal", LocalDateTime.now(), LocalDateTime.now().plusMonths(1), false));

        assertThat(plan.currency()).isEqualTo("USD");
        assertThat(subscription.organizationDomain()).isEqualTo("billing.example.com");
        assertThat(billingService.listOrganizationSubscriptions(billingUser.id(), organization.id()))
                .extracting(ApiPayloads.BillingSubscriptionResponse::planCode)
                .contains("team-monthly");
    }
}
