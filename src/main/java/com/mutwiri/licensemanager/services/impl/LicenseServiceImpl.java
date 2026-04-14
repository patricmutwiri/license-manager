/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 11:32 PM
 *
 */

package com.mutwiri.licensemanager.services.impl;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.exceptions.LicenseGenerationException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.EmailService;
import com.mutwiri.licensemanager.services.LicenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class LicenseServiceImpl implements LicenseService {

    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final EmailService emailService;

    public LicenseServiceImpl(LicenseRepository licenseRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            EmailService emailService) {
        this.licenseRepository = licenseRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.emailService = emailService;
    }

    @Override
    public License generateLicense(Long userId, Long organizationId) {
        return generateLicense(userId, organizationId, null, "Default App", null, LocalDateTime.now().plusYears(1),
                new HashMap<>());
    }

    @Override
    public License generateLicense(Long userId, Long organizationId, String hostname, String applicationName,
            String email, LocalDateTime expiryDate, Map<String, String> customFields) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User with ID " + userId + " not found"));
            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Organization with ID " + organizationId + " not found"));

            License license = new License();
            license.setKey(UUID.randomUUID().toString());
            license.setExpiry(expiryDate != null ? expiryDate : LocalDateTime.now().plusYears(1));
            license.setStatus(LicenseStatus.ACTIVE);
            license.setActive(true);
            license.setHostname(hostname);
            license.setApplicationName(applicationName);
            license.setEmail(email);
            license.setCustomFields(customFields != null ? customFields : new HashMap<>());
            license.setUser(user);
            license.setOrganization(org);

            License saved = licenseRepository.save(license);
            log.info("License generated successfully: key={}, userId={}, orgId={}", saved.getKey(), userId, organizationId);

            // Trigger backup email asynchronously
            emailService.sendLicenseBackupAsync(saved);
            return saved;
        } catch (ResourceNotFoundException e) {
            log.error("Failed to generate license: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during license generation", e);
            throw new LicenseGenerationException("Failed to generate license", e);
        }
    }

    @Override
    public boolean validateLicense(String key) {
        try {
            return licenseRepository.findByKey(key)
                    .map(license -> license.isActive()
                            && license.getStatus() == LicenseStatus.ACTIVE
                            && license.getExpiry() != null
                            && license.getExpiry().isAfter(LocalDateTime.now()))
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error validating license key: {}", key, e);
            return false;
        }
    }

    @Override
    public List<License> getLicensesByUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found"));
        return licenseRepository.findByUserId(userId);
    }

    @Override
    public List<License> getLicensesByOrganization(Long organizationId) {
        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization with ID " + organizationId + " not found"));
        return licenseRepository.findByOrganizationId(organizationId);
    }

    @Override
    public Optional<License> getLicenseByKey(String key) {
        return licenseRepository.findByKey(key);
    }
}
