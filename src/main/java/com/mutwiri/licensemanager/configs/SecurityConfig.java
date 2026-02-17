/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.mutwiri.licensemanager.services.UserService;

@Configuration
public class SecurityConfig {
        private final UserService userService;

        public SecurityConfig(UserService userService) {
                this.userService = userService;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers("/", "/login", "/register", "/error",
                                                                "/organizations", "/license/validate", "/css/**",
                                                                "/js/**", "/images/**")
                                                .permitAll() // Public access
                                                .anyRequest().authenticated() // Secure everything else
                                )
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(userService)))
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/") // Redirect here after logout
                                );
                return http.build();
        }
}
