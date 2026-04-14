/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.BillingPlan;
import com.mutwiri.licensemanager.entities.BillingProvider;
import com.mutwiri.licensemanager.entities.BillingSubscription;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.Permission;
import com.mutwiri.licensemanager.entities.Policy;
import com.mutwiri.licensemanager.entities.SubscriptionStatus;
import com.mutwiri.licensemanager.exceptions.ConflictException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.repository.BillingPlanRepository;
import com.mutwiri.licensemanager.repository.BillingSubscriptionRepository;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BillingService {
    private final BillingPlanRepository planRepository;
    private final BillingSubscriptionRepository subscriptionRepository;
    private final PolicyRepository policyRepository;
    private final OrganizationRepository organizationRepository;
    private final RbacService rbacService;
    private final AuditService auditService;

    public BillingService(BillingPlanRepository planRepository,
            BillingSubscriptionRepository subscriptionRepository,
            PolicyRepository policyRepository,
            OrganizationRepository organizationRepository,
            RbacService rbacService,
            AuditService auditService) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.policyRepository = policyRepository;
        this.organizationRepository = organizationRepository;
        this.rbacService = rbacService;
        this.auditService = auditService;
    }

    @Transactional
    public ApiPayloads.BillingPlanResponse createPlan(Long actorUserId, ApiPayloads.CreateBillingPlanRequest request) {
        rbacService.requireGlobal(actorUserId, Permission.BILLING_MANAGE);
        if (planRepository.existsByCode(request.code())) {
            throw new ConflictException("Billing plan code already exists.");
        }
        Policy policy = policy(request.policyId());
        BillingPlan plan = new BillingPlan();
        plan.setCode(normalizeCode(request.code()));
        plan.setName(request.name().trim());
        plan.setPolicy(policy);
        plan.setAmountCents(request.amountCents());
        plan.setCurrency(request.currency().trim().toUpperCase());
        plan.setBillingInterval(request.billingInterval());
        plan.setTrialDays(Math.max(0, request.trialDays()));
        plan.setProvider(request.provider() == null ? BillingProvider.INTERNAL : request.provider());
        plan.setProviderPriceId(blankToNull(request.providerPriceId()));
        plan.setMetadata(request.metadata() == null ? new HashMap<>() : new HashMap<>(request.metadata()));
        BillingPlan saved = planRepository.save(plan);
        auditService.record("billing.plan_created", "admin-api", "billing_plan", saved.getId().toString(),
                "Billing plan created", Map.of("code", saved.getCode(), "provider", saved.getProvider().name()));
        return toPlanResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.BillingPlanResponse> listPlans(Long actorUserId) {
        rbacService.requireGlobal(actorUserId, Permission.BILLING_MANAGE);
        return planRepository.findAll().stream().map(this::toPlanResponse).toList();
    }

    @Transactional
    public ApiPayloads.BillingSubscriptionResponse createSubscription(
            Long actorUserId, ApiPayloads.CreateBillingSubscriptionRequest request) {
        rbacService.requireOrganization(actorUserId, request.organizationId(), Permission.BILLING_MANAGE);
        Organization organization = organization(request.organizationId());
        BillingPlan plan = plan(request.planId());
        BillingSubscription subscription = new BillingSubscription();
        subscription.setOrganization(organization);
        subscription.setPlan(plan);
        subscription.setStatus(request.status() == null ? SubscriptionStatus.ACTIVE : request.status());
        subscription.setProvider(request.provider() == null ? plan.getProvider() : request.provider());
        subscription.setProviderCustomerId(blankToNull(request.providerCustomerId()));
        subscription.setProviderSubscriptionId(blankToNull(request.providerSubscriptionId()));
        subscription.setCurrentPeriodStart(request.currentPeriodStart() == null ? LocalDateTime.now() : request.currentPeriodStart());
        subscription.setCurrentPeriodEnd(request.currentPeriodEnd());
        subscription.setCancelAtPeriodEnd(request.cancelAtPeriodEnd());
        BillingSubscription saved = subscriptionRepository.save(subscription);
        auditService.record("billing.subscription_created", "admin-api", "billing_subscription", saved.getId().toString(),
                "Billing subscription created", Map.of("organization", organization.getDomain(), "plan", plan.getCode()));
        return toSubscriptionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.BillingSubscriptionResponse> listOrganizationSubscriptions(Long actorUserId, Long organizationId) {
        rbacService.requireOrganization(actorUserId, organizationId, Permission.BILLING_MANAGE);
        return subscriptionRepository.findByOrganizationId(organizationId).stream()
                .map(this::toSubscriptionResponse)
                .toList();
    }

    private BillingPlan plan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Billing plan with ID " + planId + " not found"));
    }

    private Policy policy(Long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy with ID " + policyId + " not found"));
    }

    private Organization organization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization with ID " + organizationId + " not found"));
    }

    private ApiPayloads.BillingPlanResponse toPlanResponse(BillingPlan plan) {
        return new ApiPayloads.BillingPlanResponse(plan.getId(), plan.getCode(), plan.getName(),
                plan.getPolicy().getId(), plan.getPolicy().getCode(), plan.getAmountCents(), plan.getCurrency(),
                plan.getBillingInterval(), plan.getTrialDays(), plan.isActive(), plan.getProvider(),
                plan.getProviderPriceId(), plan.getMetadata());
    }

    private ApiPayloads.BillingSubscriptionResponse toSubscriptionResponse(BillingSubscription subscription) {
        return new ApiPayloads.BillingSubscriptionResponse(subscription.getId(),
                subscription.getOrganization().getId(), subscription.getOrganization().getDomain(),
                subscription.getPlan().getId(), subscription.getPlan().getCode(), subscription.getStatus(),
                subscription.getProvider(), subscription.getProviderCustomerId(), subscription.getProviderSubscriptionId(),
                subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd(), subscription.isCancelAtPeriodEnd());
    }

    private String normalizeCode(String code) {
        return code.trim().toLowerCase().replaceAll("[^a-z0-9._-]", "-");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
