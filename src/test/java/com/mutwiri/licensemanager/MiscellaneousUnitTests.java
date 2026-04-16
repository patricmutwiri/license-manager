/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.exceptions.ErrorResponse;
import com.mutwiri.licensemanager.exceptions.LicenseGenerationException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.EmailService;
import com.mutwiri.licensemanager.services.impl.LicenseServiceImpl;
import com.mutwiri.licensemanager.services.impl.OrganizationServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MiscellaneousUnitTests {
    @Test
    void shouldExposeMutableErrorResponseFieldsAndExceptionCauses() {
        ErrorResponse legacy = new ErrorResponse("OLD", "old", 123L);
        legacy.setCode("NEW");
        legacy.setMessage("message");
        legacy.setStatus(409);
        legacy.setPath("/path");
        legacy.setRequestId("req-1");
        legacy.setTimestamp(456L);

        assertThat(legacy.getCode()).isEqualTo("NEW");
        assertThat(legacy.getMessage()).isEqualTo("message");
        assertThat(legacy.getStatus()).isEqualTo(409);
        assertThat(legacy.getPath()).isEqualTo("/path");
        assertThat(legacy.getRequestId()).isEqualTo("req-1");
        assertThat(legacy.getTimestamp()).isEqualTo(456L);
        assertThat(new ErrorResponse("ERR", "full", 500, "/x", "req", 789L).getRequestId()).isEqualTo("req");
        assertThat(new LicenseGenerationException("bad", new IllegalStateException("cause")).getCause())
                .isInstanceOf(IllegalStateException.class);
        assertThat(new ResourceNotFoundException("missing", new IllegalArgumentException("cause")).getCause())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDelegateOrganizationServiceRepositoryOperations() {
        OrganizationRepository repository = mock(OrganizationRepository.class);
        OrganizationServiceImpl service = new OrganizationServiceImpl(repository);
        Organization organization = organization();
        when(repository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findAll()).thenReturn(List.of(organization));
        when(repository.findById(1L)).thenReturn(Optional.of(organization));
        when(repository.findByDomain("example.com")).thenReturn(Optional.of(organization));

        assertThat(service.createOrganization("Org", "ops@example.com", "example.com").getDomain())
                .isEqualTo("example.com");
        assertThat(service.getAllOrganizations()).containsExactly(organization);
        assertThat(service.getOrganizationById(1L)).contains(organization);
        assertThat(service.getOrganizationByDomain("example.com")).contains(organization);
    }

    @Test
    void shouldCoverLegacyLicenseServiceValidationAndFailureBranches() {
        LicenseRepository licenseRepository = mock(LicenseRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        EmailService emailService = mock(EmailService.class);
        LicenseServiceImpl service = new LicenseServiceImpl(
                licenseRepository, userRepository, organizationRepository, emailService);
        License active = license(true, LicenseStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        License expired = license(true, LicenseStatus.ACTIVE, LocalDateTime.now().minusDays(1));
        License inactive = license(false, LicenseStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        License suspended = license(true, LicenseStatus.SUSPENDED, LocalDateTime.now().plusDays(1));
        User user = new User();
        Organization organization = organization();

        when(licenseRepository.findByKey("active")).thenReturn(Optional.of(active));
        when(licenseRepository.findByKey("expired")).thenReturn(Optional.of(expired));
        when(licenseRepository.findByKey("inactive")).thenReturn(Optional.of(inactive));
        when(licenseRepository.findByKey("suspended")).thenReturn(Optional.of(suspended));
        when(licenseRepository.findByKey("error")).thenThrow(new IllegalStateException("db"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(404L)).thenReturn(Optional.empty());
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(organization));
        when(organizationRepository.findById(404L)).thenReturn(Optional.empty());
        when(licenseRepository.findByUserId(1L)).thenReturn(List.of(active));
        when(licenseRepository.findByOrganizationId(2L)).thenReturn(List.of(active));
        when(licenseRepository.save(any(License.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.validateLicense("active")).isTrue();
        assertThat(service.validateLicense("expired")).isFalse();
        assertThat(service.validateLicense("inactive")).isFalse();
        assertThat(service.validateLicense("suspended")).isFalse();
        assertThat(service.validateLicense("missing")).isFalse();
        assertThat(service.validateLicense("error")).isFalse();
        assertThat(service.getLicensesByUser(1L)).containsExactly(active);
        assertThat(service.getLicensesByOrganization(2L)).containsExactly(active);
        assertThat(service.getLicenseByKey("active")).contains(active);
        assertThat(service.generateLicense(1L, 2L, "host", "App", "customer@example.com", null, null).getCustomFields())
                .isEqualTo(Map.of());
        assertThatThrownBy(() -> service.getLicensesByUser(404L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getLicensesByOrganization(404L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.generateLicense(404L, 2L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.generateLicense(1L, 404L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private Organization organization() {
        Organization organization = new Organization();
        organization.setName("Org");
        organization.setEmail("ops@example.com");
        organization.setDomain("example.com");
        return organization;
    }

    private License license(boolean active, LicenseStatus status, LocalDateTime expiry) {
        License license = new License();
        license.setKey("key");
        license.setActive(active);
        license.setStatus(status);
        license.setExpiry(expiry);
        return license;
    }
}
