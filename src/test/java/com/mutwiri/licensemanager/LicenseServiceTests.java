/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.LicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LicenseServiceTests {

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setProvider("google");
        testUser.setProviderId("12345");
        testUser = userRepository.save(testUser);

        testOrg = new Organization();
        testOrg.setName("Test Org");
        testOrg.setEmail("org@example.com");
        testOrg.setDomain("example.com");
        testOrg = organizationRepository.save(testOrg);
    }

    @Test
    void testGenerateAndValidateLicense() {
        License license = licenseService.generateLicense(testUser.getId(), testOrg.getId(),
                "test-host.local", "Test App", "user@test.com", null, null);

        assertNotNull(license);
        assertNotNull(license.getKey());
        assertNotNull(license.getExpiry());
        assertEquals(testUser.getId(), license.getUser().getId());
        assertEquals(testOrg.getId(), license.getOrganization().getId());

        // License should be valid (expires in future, which is 1 year by default)
        boolean isValid = licenseService.validateLicense(license.getKey());
        assertTrue(isValid, "License should be valid as it expires in the future");

        assertFalse(licenseService.validateLicense("non-existent-key"));
    }

    @Test
    void testGetLicensesByOrg() {
        licenseService.generateLicense(testUser.getId(), testOrg.getId(),
                "test-host.local", "Test App 2", "user2@test.com", null, null);

        List<License> licenses = licenseService.getLicensesByOrganization(testOrg.getId());
        assertEquals(1, licenses.size());
        assertEquals(testOrg.getName(), licenses.get(0).getOrganization().getName());
    }
}
