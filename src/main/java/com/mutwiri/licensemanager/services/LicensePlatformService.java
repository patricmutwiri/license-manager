/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.AuditEvent;
import com.mutwiri.licensemanager.entities.ClientApiToken;
import com.mutwiri.licensemanager.entities.Entitlement;
import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.LicensingModel;
import com.mutwiri.licensemanager.entities.Machine;
import com.mutwiri.licensemanager.entities.MachineStatus;
import com.mutwiri.licensemanager.entities.OfflineLicenseArtifact;
import com.mutwiri.licensemanager.entities.OrganizationMembership;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.Permission;
import com.mutwiri.licensemanager.entities.Policy;
import com.mutwiri.licensemanager.entities.Product;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.exceptions.ConflictException;
import com.mutwiri.licensemanager.exceptions.InvalidLicenseRequestException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.repository.ClientApiTokenRepository;
import com.mutwiri.licensemanager.repository.EntitlementRepository;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.MachineRepository;
import com.mutwiri.licensemanager.repository.OfflineLicenseArtifactRepository;
import com.mutwiri.licensemanager.repository.OrganizationMembershipRepository;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.PolicyRepository;
import com.mutwiri.licensemanager.repository.ProductRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LicensePlatformService {
    private static final String ADMIN_ACTOR = "admin-api";

    private final ProductRepository productRepository;
    private final PolicyRepository policyRepository;
    private final EntitlementRepository entitlementRepository;
    private final LicenseRepository licenseRepository;
    private final MachineRepository machineRepository;
    private final OfflineLicenseArtifactRepository artifactRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final ClientApiTokenRepository clientTokenRepository;
    private final AuditService auditService;
    private final CryptoService cryptoService;
    private final RbacService rbacService;
    private final ObjectMapper objectMapper;

    public LicensePlatformService(ProductRepository productRepository,
            PolicyRepository policyRepository,
            EntitlementRepository entitlementRepository,
            LicenseRepository licenseRepository,
            MachineRepository machineRepository,
            OfflineLicenseArtifactRepository artifactRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            ClientApiTokenRepository clientTokenRepository,
            AuditService auditService,
            CryptoService cryptoService,
            RbacService rbacService,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.policyRepository = policyRepository;
        this.entitlementRepository = entitlementRepository;
        this.licenseRepository = licenseRepository;
        this.machineRepository = machineRepository;
        this.artifactRepository = artifactRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.clientTokenRepository = clientTokenRepository;
        this.auditService = auditService;
        this.cryptoService = cryptoService;
        this.rbacService = rbacService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApiPayloads.UserResponse createUser(ApiPayloads.CreateUserRequest request) {
        return createUser(null, request);
    }

    @Transactional
    public ApiPayloads.UserResponse createUser(Long actorUserId, ApiPayloads.CreateUserRequest request) {
        rbacService.requireGlobal(actorUserId, Permission.USER_MANAGE);
        String email = request.email().trim().toLowerCase();
        String name = request.name().trim();
        if (userRepository.existsByEmail(email) || userRepository.existsByName(name)) {
            throw new ConflictException("User name or email already exists.");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole(request.role() == null ? UserRole.CUSTOMER : request.role());
        user.setProvider(blankToNull(request.provider()));
        user.setProviderId(blankToNull(request.providerId()));
        User saved = userRepository.save(user);
        auditService.record("user.created", ADMIN_ACTOR, "user", saved.getId().toString(),
                "User created", Map.of("email", saved.getEmail(), "role", saved.getRole().name()));
        return toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.UserResponse> listUsers() {
        return listUsers(null);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.UserResponse> listUsers(Long actorUserId) {
        rbacService.requireGlobal(actorUserId, Permission.USER_MANAGE);
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    @Transactional
    public ApiPayloads.OrganizationResponse createOrganization(ApiPayloads.CreateOrganizationRequest request) {
        return createOrganization(null, request);
    }

    @Transactional
    public ApiPayloads.OrganizationResponse createOrganization(Long actorUserId, ApiPayloads.CreateOrganizationRequest request) {
        rbacService.requireGlobal(actorUserId, Permission.ORGANIZATION_UPDATE);
        String name = request.name().trim();
        String email = request.email().trim().toLowerCase();
        String domain = request.domain().trim().toLowerCase();
        if (organizationRepository.existsByName(name)
                || organizationRepository.existsByEmail(email)
                || organizationRepository.existsByDomain(domain)) {
            throw new ConflictException("Organization name, email, or domain already exists.");
        }
        Organization organization = new Organization();
        organization.setName(name);
        organization.setEmail(email);
        organization.setDomain(domain);
        Organization saved = organizationRepository.save(organization);
        auditService.record("organization.created", ADMIN_ACTOR, "organization", saved.getId().toString(),
                "Organization created", Map.of("domain", saved.getDomain()));
        return toOrganizationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.OrganizationResponse> listOrganizations() {
        return listOrganizations(null);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.OrganizationResponse> listOrganizations(Long actorUserId) {
        rbacService.requireGlobal(actorUserId, Permission.ORGANIZATION_READ);
        return organizationRepository.findAll().stream().map(this::toOrganizationResponse).toList();
    }

    @Transactional
    public ApiPayloads.MembershipResponse createMembership(Long actorUserId, ApiPayloads.CreateMembershipRequest request) {
        rbacService.requireOrganization(actorUserId, request.organizationId(), Permission.MEMBERSHIP_MANAGE);
        if (membershipRepository.existsByOrganizationIdAndUserId(request.organizationId(), request.userId())) {
            return membershipRepository.findByOrganizationIdAndUserId(request.organizationId(), request.userId())
                    .map(this::toMembershipResponse)
                    .orElseThrow(() -> new ConflictException("User is already a member of this organization."));
        }
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + request.userId() + " not found"));
        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization with ID " + request.organizationId() + " not found"));
        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(user);
        membership.setOrganization(organization);
        membership.setRole(request.role());
        OrganizationMembership saved = membershipRepository.save(membership);
        auditService.record("membership.created", ADMIN_ACTOR, "organization_membership", saved.getId().toString(),
                "Organization membership created", Map.of("role", saved.getRole().name()));
        return toMembershipResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.MembershipResponse> listMemberships(Long actorUserId, Long organizationId) {
        rbacService.requireOrganization(actorUserId, organizationId, Permission.MEMBERSHIP_MANAGE);
        return membershipRepository.findByOrganizationId(organizationId).stream()
                .map(this::toMembershipResponse)
                .toList();
    }

    @Transactional
    public ApiPayloads.ProductResponse createProduct(ApiPayloads.CreateProductRequest request) {
        return createProduct(null, request);
    }

    @Transactional
    public ApiPayloads.ProductResponse createProduct(Long actorUserId, ApiPayloads.CreateProductRequest request) {
        Organization organization = request.organizationId() == null ? null : organization(request.organizationId());
        if (organization == null) {
            rbacService.requireGlobal(actorUserId, Permission.PRODUCT_MANAGE);
        } else {
            rbacService.requireOrganization(actorUserId, organization.getId(), Permission.PRODUCT_MANAGE);
        }
        if (productRepository.existsByCode(request.code())) {
            throw new ConflictException("Product code already exists.");
        }
        Product product = new Product();
        product.setCode(normalizeCode(request.code()));
        product.setName(request.name().trim());
        product.setOrganization(organization);
        product.setDescription(request.description());
        product.setMetadata(request.metadata() == null ? new HashMap<>() : new HashMap<>(request.metadata()));
        Product saved = productRepository.save(product);
        auditService.record("product.created", ADMIN_ACTOR, "product", saved.getId().toString(),
                "Product created", auditMetadata("code", saved.getCode(), "organizationId",
                        organization == null ? null : organization.getId().toString()));
        return toProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.ProductResponse> listProducts() {
        return listProducts(null);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.ProductResponse> listProducts(Long actorUserId) {
        if (actorUserId != null && !rbacService.hasGlobal(actorUserId, Permission.PRODUCT_MANAGE)) {
            return membershipRepository.findByUserId(actorUserId).stream()
                    .filter(membership -> membership.getRole().grants(Permission.PRODUCT_MANAGE))
                    .flatMap(membership -> productRepository.findByOrganizationId(membership.getOrganization().getId()).stream())
                    .map(this::toProductResponse)
                    .toList();
        }
        return productRepository.findAll().stream().map(this::toProductResponse).toList();
    }

    @Transactional
    public ApiPayloads.EntitlementResponse createEntitlement(Long productId, ApiPayloads.CreateEntitlementRequest request) {
        return createEntitlement(null, productId, request);
    }

    @Transactional
    public ApiPayloads.EntitlementResponse createEntitlement(Long actorUserId, Long productId, ApiPayloads.CreateEntitlementRequest request) {
        Product product = product(productId);
        requireProductPermission(actorUserId, product, Permission.PRODUCT_MANAGE);
        String code = normalizeCode(request.code());
        entitlementRepository.findByProductIdAndCode(productId, code).ifPresent(existing -> {
            throw new ConflictException("Entitlement code already exists for this product.");
        });
        Entitlement entitlement = new Entitlement();
        entitlement.setProduct(product);
        entitlement.setCode(code);
        entitlement.setName(request.name().trim());
        entitlement.setDescription(request.description());
        Entitlement saved = entitlementRepository.save(entitlement);
        auditService.record("entitlement.created", ADMIN_ACTOR, "entitlement", saved.getId().toString(),
                "Entitlement created", Map.of("code", saved.getCode(), "product", product.getCode()));
        return toEntitlementResponse(saved);
    }

    @Transactional
    public ApiPayloads.PolicyResponse createPolicy(ApiPayloads.CreatePolicyRequest request) {
        return createPolicy(null, request);
    }

    @Transactional
    public ApiPayloads.PolicyResponse createPolicy(Long actorUserId, ApiPayloads.CreatePolicyRequest request) {
        if (policyRepository.existsByCode(request.code())) {
            throw new ConflictException("Policy code already exists.");
        }
        Product product = product(request.productId());
        requireProductPermission(actorUserId, product, Permission.POLICY_MANAGE);
        Policy policy = new Policy();
        policy.setProduct(product);
        policy.setCode(normalizeCode(request.code()));
        policy.setName(request.name().trim());
        policy.setLicensingModel(request.licensingModel());
        policy.setMaxMachines(Math.max(1, request.maxMachines()));
        policy.setMaxSeats(Math.max(1, request.maxSeats()));
        policy.setValidityDays(Math.max(1, request.validityDays()));
        policy.setHeartbeatIntervalMinutes(Math.max(1, request.heartbeatIntervalMinutes()));
        policy.setHeartbeatGracePeriodMinutes(Math.max(1, request.heartbeatGracePeriodMinutes()));
        policy.setOfflineTtlDays(Math.max(1, request.offlineTtlDays()));
        policy.setMinVersion(blankToNull(request.minVersion()));
        policy.setMaxVersion(blankToNull(request.maxVersion()));
        policy.setEntitlementCodes(request.entitlementCodes() == null ? Set.of() : Set.copyOf(request.entitlementCodes()));
        Policy saved = policyRepository.save(policy);
        auditService.record("policy.created", ADMIN_ACTOR, "policy", saved.getId().toString(),
                "Policy created", Map.of("code", saved.getCode(), "product", product.getCode()));
        return toPolicyResponse(saved);
    }

    @Transactional(readOnly = true)
    public void authorizeClientTokenCreation(Long actorUserId, ApiPayloads.CreateClientTokenRequest request) {
        if (request.productId() == null) {
            rbacService.requireGlobal(actorUserId, Permission.CLIENT_TOKEN_MANAGE);
            return;
        }
        requireProductPermission(actorUserId, product(request.productId()), Permission.CLIENT_TOKEN_MANAGE);
    }

    @Transactional(readOnly = true)
    public void authorizeClientTokenLifecycle(Long actorUserId, Long tokenId) {
        ClientApiToken token = clientTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Client token with ID " + tokenId + " not found"));
        if (token.getProduct() == null) {
            rbacService.requireGlobal(actorUserId, Permission.CLIENT_TOKEN_MANAGE);
            return;
        }
        requireProductPermission(actorUserId, token.getProduct(), Permission.CLIENT_TOKEN_MANAGE);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.PolicyResponse> listPolicies() {
        return listPolicies(null);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.PolicyResponse> listPolicies(Long actorUserId) {
        rbacService.requireGlobal(actorUserId, Permission.POLICY_MANAGE);
        return policyRepository.findAll().stream().map(this::toPolicyResponse).toList();
    }

    @Transactional
    public ApiPayloads.LicenseLifecycleResponse issueLicense(ApiPayloads.IssueLicenseRequest request) {
        return issueLicense(null, request);
    }

    @Transactional
    public ApiPayloads.LicenseLifecycleResponse issueLicense(Long actorUserId, ApiPayloads.IssueLicenseRequest request) {
        rbacService.requireOrganization(actorUserId, request.organizationId(), Permission.LICENSE_ISSUE);
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + request.userId() + " not found"));
        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization with ID " + request.organizationId() + " not found"));
        Policy policy = policy(request.policyId());

        License license = new License();
        license.setKey("lic_" + UUID.randomUUID());
        license.setUser(user);
        license.setOrganization(organization);
        license.setProduct(policy.getProduct());
        license.setPolicy(policy);
        license.setStatus(LicenseStatus.ACTIVE);
        license.setActive(true);
        license.setExpiry(request.expiry() == null ? LocalDateTime.now().plusDays(policy.getValidityDays()) : request.expiry());
        license.setApplicationName(firstNonBlank(request.applicationName(), policy.getProduct().getName()));
        license.setEmail(firstNonBlank(request.email(), request.customerEmail(), user.getEmail()));
        license.setCustomerName(firstNonBlank(request.customerName(), user.getName()));
        license.setCustomerEmail(firstNonBlank(request.customerEmail(), user.getEmail()));
        license.setCustomFields(request.metadata() == null ? new HashMap<>() : new HashMap<>(request.metadata()));
        License saved = licenseRepository.save(license);
        auditService.record("license.issued", ADMIN_ACTOR, "license", saved.getId().toString(),
                "License issued", Map.of("key", saved.getKey(), "policy", policy.getCode()));
        return toLicenseLifecycleResponse(saved);
    }

    @Transactional
    public ApiPayloads.LicenseLifecycleResponse changeLicenseStatus(Long licenseId, LicenseStatus status) {
        return changeLicenseStatus(null, licenseId, status);
    }

    @Transactional
    public ApiPayloads.LicenseLifecycleResponse changeLicenseStatus(Long actorUserId, Long licenseId, LicenseStatus status) {
        License license = license(licenseId);
        if (license.getOrganization() != null) {
            rbacService.requireOrganization(actorUserId, license.getOrganization().getId(), Permission.LICENSE_UPDATE);
        }
        applyStatus(license, status);
        License saved = licenseRepository.save(license);
        auditService.record("license.status_changed", ADMIN_ACTOR, "license", saved.getId().toString(),
                "License status changed", Map.of("status", status.name()));
        return toLicenseLifecycleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.LicenseLifecycleResponse> listLicenses() {
        return listLicenses(null);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.LicenseLifecycleResponse> listLicenses(Long actorUserId) {
        rbacService.requireGlobal(actorUserId, Permission.LICENSE_READ);
        return licenseRepository.findAll().stream().map(this::toLicenseLifecycleResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.LicenseLifecycleResponse> listOrganizationLicenses(Long actorUserId, Long organizationId) {
        rbacService.requireOrganization(actorUserId, organizationId, Permission.LICENSE_READ);
        return licenseRepository.findByOrganizationId(organizationId).stream()
                .map(this::toLicenseLifecycleResponse)
                .toList();
    }

    @Transactional
    public ApiPayloads.ValidationResponse validate(ApiPayloads.ValidationRequest request) {
        License license = licenseRepository.findByKey(request.key())
                .orElse(null);
        if (license == null) {
            return invalid("LICENSE_NOT_FOUND", "License key was not found.", request.key(), null, null);
        }
        normalizeExpiry(license);
        ApiPayloads.ValidationResponse base = validateLicenseState(license, request);
        if (!base.valid()) {
            return base;
        }
        if (request.fingerprint() == null || request.fingerprint().isBlank()) {
            return valid(license, null, "VALID", "License is valid without machine scope.");
        }
        Machine machine = machineRepository.findByLicenseIdAndFingerprintHash(license.getId(), fingerprintHash(request.fingerprint()))
                .orElse(null);
        if (machine == null || machine.getStatus() == MachineStatus.DEACTIVATED) {
            return valid(license, null, "MACHINE_NOT_ACTIVATED", "License is valid but this machine is not activated.");
        }
        reclaimIfHeartbeatMissed(machine, license.getPolicy(), LocalDateTime.now());
        if (machine.getStatus() == MachineStatus.HEARTBEAT_MISSED) {
            return invalid("HEARTBEAT_MISSED", "Machine heartbeat grace period has elapsed.", license.getKey(), license, machine);
        }
        return valid(license, machine, "VALID", "License and machine are valid.");
    }

    @Transactional
    public ApiPayloads.MachineResponse activate(String licenseKey, ApiPayloads.ActivationRequest request) {
        License license = licenseByKey(licenseKey);
        normalizeExpiry(license);
        ApiPayloads.ValidationResponse state = validateLicenseState(license,
                new ApiPayloads.ValidationRequest(licenseKey, null, null, null, request.version()));
        if (!state.valid()) {
            throw new InvalidLicenseRequestException(state.detail());
        }
        Policy policy = requirePolicy(license);
        String fingerprintHash = fingerprintHash(request.fingerprint());
        Machine existing = machineRepository.findByLicenseIdAndFingerprintHash(license.getId(), fingerprintHash).orElse(null);
        if (existing != null) {
            updateMachine(existing, request, MachineStatus.ACTIVE);
            auditService.record("machine.activated", "client", "machine", existing.getId().toString(),
                    "Machine activation refreshed", Map.of("license", license.getKey()));
            return toMachineResponse(machineRepository.save(existing));
        }
        reclaimDeadSeats(license);
        enforceActivationLimit(license, policy);

        Machine machine = new Machine();
        machine.setLicense(license);
        machine.setFingerprint(request.fingerprint());
        machine.setFingerprintHash(fingerprintHash);
        updateMachine(machine, request, MachineStatus.ACTIVE);
        Machine saved = machineRepository.save(machine);
        auditService.record("machine.activated", "client", "machine", saved.getId().toString(),
                "Machine activated", Map.of("license", license.getKey()));
        return toMachineResponse(saved);
    }

    @Transactional
    public ApiPayloads.MachineResponse heartbeat(String licenseKey, ApiPayloads.HeartbeatRequest request) {
        License license = licenseByKey(licenseKey);
        Machine machine = machineRepository.findByLicenseIdAndFingerprintHash(license.getId(), fingerprintHash(request.fingerprint()))
                .orElseThrow(() -> new ResourceNotFoundException("Machine is not activated for this license."));
        machine.setLastSeenAt(LocalDateTime.now());
        machine.setLastHeartbeatAt(LocalDateTime.now());
        machine.setStatus(MachineStatus.ACTIVE);
        machine.setVersion(blankToNull(request.version()));
        Machine saved = machineRepository.save(machine);
        auditService.record("machine.heartbeat", "client", "machine", saved.getId().toString(),
                "Machine heartbeat accepted", Map.of("license", license.getKey()));
        return toMachineResponse(saved);
    }

    @Transactional
    public ApiPayloads.MachineResponse deactivate(String licenseKey, String fingerprint) {
        License license = licenseByKey(licenseKey);
        Machine machine = machineRepository.findByLicenseIdAndFingerprintHash(license.getId(), fingerprintHash(fingerprint))
                .orElseThrow(() -> new ResourceNotFoundException("Machine is not activated for this license."));
        machine.setStatus(MachineStatus.DEACTIVATED);
        machine.setDeactivatedAt(LocalDateTime.now());
        Machine saved = machineRepository.save(machine);
        auditService.record("machine.deactivated", "client", "machine", saved.getId().toString(),
                "Machine deactivated", Map.of("license", license.getKey()));
        return toMachineResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.MachineResponse> listMachines(Long licenseId) {
        return listMachines(null, licenseId);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.MachineResponse> listMachines(Long actorUserId, Long licenseId) {
        License license = license(licenseId);
        if (license.getOrganization() != null) {
            rbacService.requireOrganization(actorUserId, license.getOrganization().getId(), Permission.MACHINE_READ);
        }
        return machineRepository.findByLicenseId(licenseId).stream().map(this::toMachineResponse).toList();
    }

    @Transactional
    public ApiPayloads.OfflineLicenseResponse checkoutOffline(ApiPayloads.OfflineCheckoutRequest request) {
        License license = licenseByKey(request.key());
        Machine machine = machineRepository.findByLicenseIdAndFingerprintHash(license.getId(), fingerprintHash(request.fingerprint()))
                .orElseThrow(() -> new ResourceNotFoundException("Machine must be activated before offline checkout."));
        ApiPayloads.ValidationResponse validation = validate(new ApiPayloads.ValidationRequest(
                license.getKey(), null, null, request.fingerprint(), machine.getVersion()));
        if (!validation.valid()) {
            throw new InvalidLicenseRequestException(validation.detail());
        }
        LocalDateTime issuedAt = LocalDateTime.now();
        int policyTtl = requirePolicy(license).getOfflineTtlDays();
        int ttlDays = request.ttlDays() == null ? policyTtl : Math.min(Math.max(1, request.ttlDays()), policyTtl);
        LocalDateTime expiresAt = List.of(issuedAt.plusDays(ttlDays), license.getExpiry()).stream()
                .filter(value -> value != null)
                .min(Comparator.naturalOrder())
                .orElse(issuedAt.plusDays(ttlDays));
        String payload = offlinePayload(license, machine, issuedAt, expiresAt);
        String artifact = cryptoService.encode(payload) + "." + cryptoService.sign(payload);

        OfflineLicenseArtifact stored = new OfflineLicenseArtifact();
        stored.setLicense(license);
        stored.setMachine(machine);
        stored.setTokenHash(cryptoService.sha256(artifact));
        stored.setExpiresAt(expiresAt);
        artifactRepository.save(stored);
        auditService.record("offline.checked_out", "client", "license", license.getId().toString(),
                "Offline license artifact checked out", Map.of("machine", machine.getFingerprintHash()));
        return new ApiPayloads.OfflineLicenseResponse(artifact, issuedAt, expiresAt);
    }

    @Transactional(readOnly = true)
    public ApiPayloads.OfflineVerifyResponse verifyOffline(ApiPayloads.OfflineVerifyRequest request) {
        try {
            String[] parts = request.artifact().split("\\.", 2);
            if (parts.length != 2) {
                return offlineInvalid("MALFORMED_ARTIFACT", "Offline artifact must contain payload and signature.");
            }
            String payload = cryptoService.decode(parts[0]);
            if (!cryptoService.verify(payload, parts[1])) {
                return offlineInvalid("BAD_SIGNATURE", "Offline artifact signature is invalid.");
            }
            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<>() {
            });
            LocalDateTime expiresAt = LocalDateTime.parse(claims.get("expiresAt").toString());
            String artifactHash = cryptoService.sha256(request.artifact());
            OfflineLicenseArtifact stored = artifactRepository.findByTokenHash(artifactHash).orElse(null);
            if (expiresAt.isBefore(LocalDateTime.now())) {
                return offlineInvalid("OFFLINE_EXPIRED", "Offline artifact has expired.");
            }
            if (stored != null && stored.isRevoked()) {
                return offlineInvalid("OFFLINE_REVOKED", "Offline artifact has been revoked.");
            }
            return new ApiPayloads.OfflineVerifyResponse(true, "VALID", "Offline artifact is valid.",
                    claims.get("licenseKey").toString(), claims.get("fingerprintHash").toString(), expiresAt);
        } catch (Exception e) {
            return offlineInvalid("INVALID_ARTIFACT", "Offline artifact could not be verified.");
        }
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.AuditEventResponse> recentAuditEvents() {
        return recentAuditEvents(null);
    }

    @Transactional(readOnly = true)
    public List<ApiPayloads.AuditEventResponse> recentAuditEvents(Long actorUserId) {
        rbacService.requireGlobal(actorUserId, Permission.AUDIT_READ);
        return auditService.recent().stream().map(this::toAuditResponse).toList();
    }

    private ApiPayloads.ValidationResponse validateLicenseState(License license, ApiPayloads.ValidationRequest request) {
        normalizeExpiry(license);
        if (license.getStatus() != LicenseStatus.ACTIVE || !license.isActive()) {
            return invalid("LICENSE_" + license.getStatus().name(), "License status is " + license.getStatus() + ".",
                    license.getKey(), license, null);
        }
        if (request.productCode() != null && license.getProduct() != null
                && !license.getProduct().getCode().equalsIgnoreCase(request.productCode())) {
            return invalid("PRODUCT_MISMATCH", "License is not valid for the requested product.", license.getKey(), license, null);
        }
        if (request.policyCode() != null && license.getPolicy() != null
                && !license.getPolicy().getCode().equalsIgnoreCase(request.policyCode())) {
            return invalid("POLICY_MISMATCH", "License is not valid for the requested policy.", license.getKey(), license, null);
        }
        if (request.version() != null && license.getPolicy() != null && !versionAllowed(license.getPolicy(), request.version())) {
            return invalid("VERSION_NOT_ALLOWED", "Client version is outside the policy bounds.", license.getKey(), license, null);
        }
        return valid(license, null, "VALID", "License is valid.");
    }

    private void enforceActivationLimit(License license, Policy policy) {
        long activeMachines = machineRepository.countByLicenseIdAndStatus(license.getId(), MachineStatus.ACTIVE);
        int limit = policy.getLicensingModel() == LicensingModel.NODE_LOCKED
                ? policy.getMaxMachines()
                : policy.getMaxSeats();
        if (activeMachines >= limit) {
            throw new ConflictException("Activation limit reached for this license.");
        }
    }

    private void reclaimDeadSeats(License license) {
        Policy policy = requirePolicy(license);
        machineRepository.findByLicenseId(license.getId()).forEach(machine -> reclaimIfHeartbeatMissed(machine, policy, LocalDateTime.now()));
    }

    private void reclaimIfHeartbeatMissed(Machine machine, Policy policy, LocalDateTime now) {
        if (machine.getStatus() != MachineStatus.ACTIVE || machine.getLastHeartbeatAt() == null || policy == null) {
            return;
        }
        long allowedMinutes = (long) policy.getHeartbeatIntervalMinutes() + policy.getHeartbeatGracePeriodMinutes();
        if (Duration.between(machine.getLastHeartbeatAt(), now).toMinutes() > allowedMinutes) {
            machine.setStatus(MachineStatus.HEARTBEAT_MISSED);
            machineRepository.save(machine);
            auditService.record("machine.heartbeat_missed", "system", "machine", machine.getId().toString(),
                    "Machine heartbeat grace period elapsed", Map.of("license", machine.getLicense().getKey()));
        }
    }

    private void updateMachine(Machine machine, ApiPayloads.ActivationRequest request, MachineStatus status) {
        machine.setName(blankToNull(request.name()));
        machine.setPlatform(blankToNull(request.platform()));
        machine.setVersion(blankToNull(request.version()));
        machine.setLastSeenAt(LocalDateTime.now());
        machine.setLastHeartbeatAt(LocalDateTime.now());
        machine.setDeactivatedAt(null);
        machine.setStatus(status);
    }

    private void normalizeExpiry(License license) {
        if (license.getExpiry() != null && license.getExpiry().isBefore(LocalDateTime.now())
                && license.getStatus() == LicenseStatus.ACTIVE) {
            license.setStatus(LicenseStatus.EXPIRED);
            license.setActive(false);
            licenseRepository.save(license);
        }
    }

    private void applyStatus(License license, LicenseStatus status) {
        license.setStatus(status);
        license.setActive(status == LicenseStatus.ACTIVE);
        if (status == LicenseStatus.REVOKED) {
            license.setRevokedAt(LocalDateTime.now());
        }
        if (status == LicenseStatus.SUSPENDED) {
            license.setSuspendedAt(LocalDateTime.now());
        }
    }

    private String offlinePayload(License license, Machine machine, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "licenseKey", license.getKey(),
                    "status", license.getStatus().name(),
                    "productCode", license.getProduct().getCode(),
                    "policyCode", license.getPolicy().getCode(),
                    "fingerprintHash", machine.getFingerprintHash(),
                    "issuedAt", issuedAt.toString(),
                    "expiresAt", expiresAt.toString(),
                    "entitlements", license.getPolicy().getEntitlementCodes()));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build offline payload", e);
        }
    }

    private ApiPayloads.ValidationResponse valid(License license, Machine machine, String code, String detail) {
        Policy policy = license.getPolicy();
        LocalDateTime nextHeartbeat = machine == null || policy == null || machine.getLastHeartbeatAt() == null
                ? null
                : machine.getLastHeartbeatAt().plusMinutes(policy.getHeartbeatIntervalMinutes());
        return new ApiPayloads.ValidationResponse(true, code, detail, license.getKey(), license.getStatus(),
                license.getProduct() == null ? null : license.getProduct().getCode(),
                policy == null ? null : policy.getCode(),
                policy == null ? List.of() : policy.getEntitlementCodes().stream().sorted().toList(),
                machine == null ? null : toMachineResponse(machine), license.getExpiry(), nextHeartbeat);
    }

    private ApiPayloads.ValidationResponse invalid(String code, String detail, String key, License license, Machine machine) {
        return new ApiPayloads.ValidationResponse(false, code, detail, key,
                license == null ? null : license.getStatus(),
                license == null || license.getProduct() == null ? null : license.getProduct().getCode(),
                license == null || license.getPolicy() == null ? null : license.getPolicy().getCode(),
                List.of(), machine == null ? null : toMachineResponse(machine),
                license == null ? null : license.getExpiry(), null);
    }

    private ApiPayloads.OfflineVerifyResponse offlineInvalid(String code, String detail) {
        return new ApiPayloads.OfflineVerifyResponse(false, code, detail, null, null, null);
    }

    private boolean versionAllowed(Policy policy, String version) {
        if (policy.getMinVersion() != null && compareVersions(version, policy.getMinVersion()) < 0) {
            return false;
        }
        return policy.getMaxVersion() == null || compareVersions(version, policy.getMaxVersion()) <= 0;
    }

    private int compareVersions(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            int leftValue = i < leftParts.length ? parseVersionPart(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? parseVersionPart(rightParts[i]) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        String digits = part.replaceAll("[^0-9]", "");
        return digits.isBlank() ? 0 : Integer.parseInt(digits);
    }

    private Product product(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));
    }

    private Organization organization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization with ID " + organizationId + " not found"));
    }

    private void requireProductPermission(Long actorUserId, Product product, Permission permission) {
        if (product.getOrganization() == null) {
            rbacService.requireGlobal(actorUserId, permission);
            return;
        }
        rbacService.requireOrganization(actorUserId, product.getOrganization().getId(), permission);
    }

    private Map<String, String> auditMetadata(String firstKey, String firstValue, String secondKey, String secondValue) {
        Map<String, String> metadata = new HashMap<>();
        if (firstValue != null) {
            metadata.put(firstKey, firstValue);
        }
        if (secondValue != null) {
            metadata.put(secondKey, secondValue);
        }
        return metadata;
    }

    private Policy policy(Long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy with ID " + policyId + " not found"));
    }

    private License license(Long licenseId) {
        return licenseRepository.findById(licenseId)
                .orElseThrow(() -> new ResourceNotFoundException("License with ID " + licenseId + " not found"));
    }

    private License licenseByKey(String key) {
        return licenseRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("License key not found"));
    }

    private Policy requirePolicy(License license) {
        if (license.getPolicy() == null) {
            throw new InvalidLicenseRequestException("License has no policy and cannot use platform activation flows.");
        }
        return license.getPolicy();
    }

    private String fingerprintHash(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            throw new InvalidLicenseRequestException("Machine fingerprint is required.");
        }
        return cryptoService.sha256(fingerprint.trim());
    }

    private String normalizeCode(String code) {
        return code.trim().toLowerCase().replaceAll("[^a-z0-9._-]", "-");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ApiPayloads.UserResponse toUserResponse(User user) {
        return new ApiPayloads.UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getProvider(), user.getProviderId(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private ApiPayloads.OrganizationResponse toOrganizationResponse(Organization organization) {
        return new ApiPayloads.OrganizationResponse(organization.getId(), organization.getName(),
                organization.getEmail(), organization.getDomain(), organization.getCreatedAt(), organization.getUpdatedAt());
    }

    private ApiPayloads.MembershipResponse toMembershipResponse(OrganizationMembership membership) {
        return new ApiPayloads.MembershipResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getOrganization().getId(),
                membership.getOrganization().getDomain(),
                membership.getRole(),
                membership.getRole().permissions(),
                membership.getCreatedAt(),
                membership.getUpdatedAt());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private ApiPayloads.ProductResponse toProductResponse(Product product) {
        return new ApiPayloads.ProductResponse(product.getId(),
                product.getOrganization() == null ? null : product.getOrganization().getId(),
                product.getOrganization() == null ? null : product.getOrganization().getDomain(),
                product.getCode(), product.getName(),
                product.getDescription(), product.getMetadata(), product.getCreatedAt(), product.getUpdatedAt());
    }

    private ApiPayloads.EntitlementResponse toEntitlementResponse(Entitlement entitlement) {
        return new ApiPayloads.EntitlementResponse(entitlement.getId(), entitlement.getCode(),
                entitlement.getName(), entitlement.getDescription());
    }

    private ApiPayloads.PolicyResponse toPolicyResponse(Policy policy) {
        return new ApiPayloads.PolicyResponse(policy.getId(), policy.getProduct().getId(),
                policy.getProduct().getCode(), policy.getCode(), policy.getName(), policy.getLicensingModel(),
                policy.getMaxMachines(), policy.getMaxSeats(), policy.getValidityDays(),
                policy.getHeartbeatIntervalMinutes(), policy.getHeartbeatGracePeriodMinutes(),
                policy.getOfflineTtlDays(), policy.getMinVersion(), policy.getMaxVersion(),
                policy.getEntitlementCodes());
    }

    private ApiPayloads.LicenseLifecycleResponse toLicenseLifecycleResponse(License license) {
        return new ApiPayloads.LicenseLifecycleResponse(license.getId(), license.getKey(), license.getStatus(),
                license.getProduct() == null ? null : license.getProduct().getCode(),
                license.getPolicy() == null ? null : license.getPolicy().getCode(), license.getExpiry(),
                license.getCustomerName(), license.getCustomerEmail(), license.getCustomFields());
    }

    private ApiPayloads.MachineResponse toMachineResponse(Machine machine) {
        return new ApiPayloads.MachineResponse(machine.getId(), machine.getFingerprintHash(), machine.getName(),
                machine.getPlatform(), machine.getVersion(), machine.getStatus(), machine.getLastSeenAt(),
                machine.getLastHeartbeatAt());
    }

    private ApiPayloads.AuditEventResponse toAuditResponse(AuditEvent event) {
        return new ApiPayloads.AuditEventResponse(event.getId(), event.getEventType(), event.getActor(),
                event.getResourceType(), event.getResourceId(), event.getDescription(), event.getMetadata(),
                event.getCreatedAt());
    }
}
