/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.repository.UserRepository;
import com.mutwiri.licensemanager.services.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminConsoleControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    @Test
    void shouldRenderOperationsConsoleForAdminUser() throws Exception {
        User admin = new User();
        admin.setName("Console Admin");
        admin.setEmail("console-admin@example.com");
        admin.setRole(UserRole.ADMIN);
        admin.setProvider("test");
        admin.setProviderId("console-admin-provider");
        userRepository.save(admin);

        mockMvc.perform(get("/admin")
                        .with(oauth2Login().attributes(attributes -> attributes.put("sub", "console-admin-provider"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Licensing Control Center")))
                .andExpect(content().string(containsString("Runtime Access, Plans, Subscriptions")));
    }
}
