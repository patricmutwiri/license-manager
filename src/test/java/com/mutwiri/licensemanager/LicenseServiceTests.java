/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.LicenseService;

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
        testOrg.setDomain("example.com");
        testOrg = organizationRepository.save(testOrg);
    }

    @Test
    void testGenerateAndValidateLicense() {
        License license = licenseService.generateLicense(testUser.getId(), testOrg.getId());

        assertNotNull(license);
        assertNotNull(license.getKey());
        assertEquals(testUser.getId(), license.getUser().getId());
        assertEquals(testOrg.getId(), license.getOrganization().getId());

        boolean isValid = licenseService.validateLicense(license.getKey());
        assertTrue(isValid);

        assertFalse(licenseService.validateLicense("non-existent-key"));
    }

    @Test
    void testGetLicensesByOrg() {
        licenseService.generateLicense(testUser.getId(), testOrg.getId());

        List<License> licenses = licenseService.getLicensesByOrganization(testOrg.getId());
        assertEquals(1, licenses.size());
        assertEquals(testOrg.getName(), licenses.get(0).getOrganization().getName());
    }
}
