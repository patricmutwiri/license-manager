/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.controllers.IndexController;
import com.mutwiri.licensemanager.controllers.LicenseController;
import com.mutwiri.licensemanager.controllers.MyErrorController;
import com.mutwiri.licensemanager.controllers.OrganizationController;
import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.LicenseStatus;
import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.models.dto.DtoMapper;
import com.mutwiri.licensemanager.models.dto.LicenseResponse;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.LicensePlatformService;
import com.mutwiri.licensemanager.services.LicenseService;
import com.mutwiri.licensemanager.services.OrganizationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.ConcurrentModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegacyWebAndDtoUnitTests {
    private final LicenseService licenseService = mock(LicenseService.class);
    private final OrganizationService organizationService = mock(OrganizationService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LicensePlatformService platformService = mock(LicensePlatformService.class);
    private final DtoMapper dtoMapper = new DtoMapper();

    @Test
    void shouldRenderIndexAndGenerateLicenseThroughLegacyBrowserFlow() {
        IndexController controller = new IndexController(licenseService, organizationService, userRepository);
        Organization organization = organization(1L);
        OAuth2User principal = principal(Map.of("id", 42, "login", "patric"));
        User user = new User();
        user.setId(99L);
        user.setName("Patrick");
        user.setEmail("patrick@example.com");

        when(organizationService.getAllOrganizations()).thenReturn(List.of(organization));
        when(userRepository.findByProviderId("42")).thenReturn(Optional.of(user));

        ConcurrentModel model = new ConcurrentModel();
        assertThat(controller.index(model, principal)).isEqualTo("index");
        assertThat(model.getAttribute("userName")).isEqualTo("patric");
        assertThat(model.getAttribute("organizations")).isEqualTo(List.of(organization));
        ConcurrentModel fallbackModel = new ConcurrentModel();
        assertThat(controller.index(fallbackModel, principal(Map.of("id", 7)))).isEqualTo("index");
        assertThat(fallbackModel.getAttribute("userName")).isEqualTo("User");
        assertThat(controller.createOrganization("Acme", "ops@example.com", "acme.example.com")).isEqualTo("redirect:/");
        assertThat(controller.showGenerateForm(1L, new ConcurrentModel())).isEqualTo("generate");
        assertThat(controller.generateLicense(1L, "App", "host", "customer@example.com", "2026-12-31",
                List.of("region", ""), List.of("emea"), principal)).isEqualTo("redirect:/licenses?orgId=1");
    }

    @Test
    void shouldHandleLoginRedirectsAndLegacyLicenseListingBranches() {
        IndexController controller = new IndexController(licenseService, organizationService, userRepository);
        Organization organization = organization(2L);
        License active = license(10L, LocalDateTime.now().plusDays(5));
        License perpetual = license(11L, null);
        User unnamedUser = new User();
        unnamedUser.setEmail("fallback@example.com");
        active.setUser(unnamedUser);
        License systemLicense = license(12L, LocalDateTime.now().plusDays(40));

        when(organizationService.getOrganizationById(2L)).thenReturn(Optional.of(organization));
        when(organizationService.getOrganizationById(404L)).thenReturn(Optional.empty());
        when(licenseService.getLicensesByOrganization(2L)).thenReturn(List.of(active, perpetual, systemLicense));

        ConcurrentModel model = new ConcurrentModel();
        assertThat(controller.login()).isEqualTo("login");
        assertThat(controller.generateLicense(1L, "App", null, "customer@example.com", null, null, null, null))
                .isEqualTo("redirect:/login");
        assertThat(controller.licenses(2L, model)).isEqualTo("licenses");
        assertThat((List<?>) model.getAttribute("licenses")).hasSize(3);
        assertThat((List<?>) model.getAttribute("licenses")).anySatisfy(item ->
                assertThat((Map<String, Object>) item).containsEntry("userName", "System User"));
        assertThat(controller.licenses(null, new ConcurrentModel())).isEqualTo("licenses");
        assertThatThrownBy(() -> controller.licenses(404L, new ConcurrentModel()))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> controller.generateLicense(1L, "App", null, "customer@example.com", null,
                null, null, principal(Map.of())))
                .isInstanceOf(IllegalArgumentException.class);

        User emailUser = new User();
        emailUser.setId(77L);
        emailUser.setName("Email User");
        emailUser.setEmail("email-user@example.com");
        when(userRepository.findByProviderId("email-provider")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("email-user@example.com")).thenReturn(Optional.of(emailUser));
        assertThat(controller.generateLicense(2L, "App", null, "email-user@example.com", "",
                List.of("tier", "extra"), List.of("pro"), principal(Map.of("id", "email-provider", "email", "email-user@example.com"))))
                .isEqualTo("redirect:/licenses?orgId=2");
    }

    @Test
    void shouldExposeLegacyRestControllersAndDtoCalculations() {
        LicenseController licenseController = new LicenseController(licenseService, dtoMapper, platformService);
        OrganizationController organizationController = new OrganizationController(organizationService);
        License license = license(1L, LocalDateTime.now().plusDays(10));
        Organization organization = organization(3L);
        ApiPayloads.ValidationResponse validation = new ApiPayloads.ValidationResponse(
                true, "VALID", "Valid", "legacy-key", LicenseStatus.ACTIVE, null, null,
                List.of(), null, null, null);

        when(licenseService.generateLicense(1L, 3L)).thenReturn(license);
        when(licenseService.getLicensesByUser(1L)).thenReturn(List.of(license));
        when(licenseService.getLicensesByOrganization(3L)).thenReturn(List.of(license));
        when(platformService.validate(new ApiPayloads.ValidationRequest("legacy-key", null, null, null, null)))
                .thenReturn(validation);
        when(organizationService.createOrganization("Acme", "ops@example.com", "acme.example.com"))
                .thenReturn(organization);
        when(organizationService.getAllOrganizations()).thenReturn(List.of(organization));
        when(organizationService.getOrganizationById(3L)).thenReturn(Optional.of(organization));
        when(organizationService.getOrganizationById(404L)).thenReturn(Optional.empty());

        assertThat(licenseController.generateLicense(1L, 3L).getStatusCode().value()).isEqualTo(201);
        assertThat(licenseController.validateLicense("legacy-key").getBody()).containsEntry("valid", true);
        assertThat(licenseController.getLicensesByUser(1L).getBody()).hasSize(1);
        assertThat(licenseController.getLicensesByOrg(3L).getBody()).hasSize(1);
        assertThat(organizationController.createOrganization("Acme", "ops@example.com", "acme.example.com"))
                .isEqualTo(organization);
        assertThat(organizationController.getAllOrganizations()).containsExactly(organization);
        assertThat(organizationController.getOrganizationById(3L)).isEqualTo(organization);
        assertThatThrownBy(() -> organizationController.getOrganizationById(404L)).isInstanceOf(RuntimeException.class);

        LicenseResponse response = dtoMapper.toLicenseResponse(license);
        assertThat(response.isExpiringSoon()).isTrue();
        assertThat(response.daysUntilExpiry()).isBetween(8L, 10L);
        assertThat(dtoMapper.toLicenseResponse(null)).isNull();
        assertThat(dtoMapper.toOrganizationResponse(organization).getDomain()).isEqualTo("acme.example.com");
        assertThat(dtoMapper.toOrganizationResponse(null)).isNull();
        assertThat(LicenseResponse.builder().expiryDate(null).build().daysUntilExpiry()).isEqualTo(-1);
    }

    @Test
    void shouldRenderSpecificErrorMessages() {
        MyErrorController controller = new MyErrorController();

        assertError(controller, 404, "404", "The page you are looking for does not exist.");
        assertError(controller, 500, "500", "Our servers are experiencing issues.");
        assertError(controller, 403, "403", "You are not authorized to view this page.");

        ConcurrentModel model = new ConcurrentModel();
        assertThat(controller.handleError(new MockHttpServletRequest(), model)).isEqualTo("error");
        assertThat(model.getAttribute("errorCode")).isEqualTo("Unknown");
    }

    private void assertError(MyErrorController controller, int status, String code, String message) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        ConcurrentModel model = new ConcurrentModel();

        assertThat(controller.handleError(request, model)).isEqualTo("error");
        assertThat(model.getAttribute("errorCode")).isEqualTo(code);
        assertThat(model.getAttribute("errorMessage")).isEqualTo(message);
    }

    private OAuth2User principal(Map<String, Object> attributes) {
        OAuth2User principal = mock(OAuth2User.class);
        attributes.forEach((key, value) -> when(principal.getAttribute(key)).thenReturn(value));
        return principal;
    }

    private Organization organization(Long id) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName("Acme");
        organization.setEmail("ops@example.com");
        organization.setDomain("acme.example.com");
        organization.setCreatedAt(LocalDateTime.now().minusDays(1));
        organization.setUpdatedAt(LocalDateTime.now());
        return organization;
    }

    private License license(Long id, LocalDateTime expiry) {
        License license = new License();
        license.setId(id);
        license.setKey("legacy-key-" + id);
        license.setApplicationName("App");
        license.setHostname("<host>");
        license.setEmail("customer@example.com");
        license.setExpiry(expiry);
        license.setActive(true);
        license.setCustomFields(Map.of("html", "<safe>"));
        license.setCreatedAt(LocalDateTime.now().minusDays(2));
        license.setUpdatedAt(LocalDateTime.now());
        return license;
    }
}
