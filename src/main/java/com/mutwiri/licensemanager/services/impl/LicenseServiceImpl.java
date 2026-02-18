/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 10:55 PM
 *
 */

package com.mutwiri.licensemanager.services.impl;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.EmailService;
import com.mutwiri.licensemanager.services.LicenseService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        License license = new License();
        license.setKey(UUID.randomUUID().toString());
        license.setExpiry(expiryDate != null ? expiryDate : LocalDateTime.now().plusYears(1));
        license.setHostname(hostname);
        license.setApplicationName(applicationName);
        license.setEmail(email);
        license.setCustomFields(customFields);
        license.setUser(user);
        license.setOrganization(org);

        License saved = licenseRepository.save(license);

        // Trigger backup email asynchronously
        CompletableFuture.runAsync(() -> emailService.sendLicenseBackup(saved));
        return saved;
    }

    @Override
    public boolean validateLicense(String key) {
        return licenseRepository.findByKey(key)
                .map(license -> license.getExpiry().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Override
    public List<License> getLicensesByUser(Long userId) {
        return licenseRepository.findByUserId(userId);
    }

    @Override
    public List<License> getLicensesByOrganization(Long organizationId) {
        return licenseRepository.findByOrganizationId(organizationId);
    }

    @Override
    public Optional<License> getLicenseByKey(String key) {
        return licenseRepository.findByKey(key);
    }
}
