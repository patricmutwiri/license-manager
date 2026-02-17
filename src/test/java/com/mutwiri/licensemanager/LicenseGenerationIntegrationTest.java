package com.mutwiri.licensemanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.EmailService;
import com.mutwiri.licensemanager.services.LicenseService;

@SpringBootTest
@Transactional
public class LicenseGenerationIntegrationTest {

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @MockitoBean
    private EmailService emailService;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setProvider("manual");
        testUser.setProviderId("test-id");
        testUser = userRepository.save(testUser);

        testOrg = new Organization();
        testOrg.setName("Test Org");
        testOrg.setDomain("test.com");
        testOrg = organizationRepository.save(testOrg);
    }

    @Test
    void testAdvancedLicenseGeneration() {
        Map<String, String> customFields = new HashMap<>();
        customFields.put("version", "2.0");
        customFields.put("type", "enterprise");

        LocalDateTime expiry = LocalDateTime.now().plusMonths(6);

        License license = licenseService.generateLicense(
                testUser.getId(),
                testOrg.getId(),
                "srv-01",
                "App-A",
                "admin@test.com",
                expiry,
                customFields);

        assertThat(license).isNotNull();
        assertThat(license.getKey()).isNotNull();
        assertThat(license.getApplicationName()).isEqualTo("App-A");
        assertThat(license.getHostname()).isEqualTo("srv-01");
        assertThat(license.getEmail()).isEqualTo("admin@test.com");
        assertThat(license.getCustomFields()).containsAllEntriesOf(customFields);

        // Verify email service was called
        verify(emailService).sendLicenseBackup(any(License.class));

        // Final verification from DB
        License saved = licenseRepository.findById(license.getId()).orElseThrow();
        assertThat(saved.getCustomFields()).hasSize(2);
    }
}
