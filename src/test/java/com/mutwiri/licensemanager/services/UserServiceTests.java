/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTests {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

    @Test
    void shouldReturnExistingUserByProviderIdentity() {
        User existing = new User();
        existing.setEmail("existing@example.com");
        when(userRepository.findByProviderAndProviderId("github", "123")).thenReturn(Optional.of(existing));

        User user = userService.synchronizeUser("github", Map.of("id", 123, "email", "ignored@example.com"));

        assertThat(user).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldCreateUserWithSubEmailAndNameFallbacks() {
        when(userRepository.findByProviderAndProviderId("oidc", "sub-1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.synchronizeUser("oidc", Map.of(
                "sub", "sub-1",
                "email", "user@example.com",
                "name", "OIDC User"));

        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getName()).isEqualTo("OIDC User");
        assertThat(user.getProvider()).isEqualTo("oidc");
        assertThat(user.getProviderId()).isEqualTo("sub-1");
    }

    @Test
    void shouldCreateLocalEmailAndPreferLoginWhenEmailIsMissing() {
        when(userRepository.findByProviderAndProviderId("github", "99")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.synchronizeUser("github", Map.of("id", 99, "login", "octocat"));

        assertThat(user.getEmail()).isEqualTo("99@github.local");
        assertThat(user.getName()).isEqualTo("octocat");
    }

    @Test
    void shouldFallbackToProviderIdWhenOauthNameIsMissing() {
        when(userRepository.findByProviderAndProviderId("oidc", "sub-2")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.synchronizeUser("oidc", Map.of("sub", "sub-2"));

        assertThat(user.getEmail()).isEqualTo("sub-2@oidc.local");
        assertThat(user.getName()).isEqualTo("sub-2");
    }

    @Test
    void shouldRejectOAuthPayloadWithoutProviderId() {
        assertThatThrownBy(() -> userService.synchronizeUser("github", Map.of("email", "bad@example.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Provider ID not found in OAuth2 attributes");
    }
}
