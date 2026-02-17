/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.services;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.repository.UserRepository;

@Service
public class UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Object providerIdObj = attributes.get("sub") != null ? attributes.get("sub") : attributes.get("id");
        if (providerIdObj == null) {
            throw new RuntimeException("Provider ID not found in OAuth2 attributes");
        }
        String providerId = providerIdObj.toString();

        Object emailObj = attributes.get("email");
        String email = (emailObj != null) ? emailObj.toString() : providerId + "@" + registrationId + ".local";

        User user = userRepository.findByProviderAndProviderId(registrationId, providerId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    Object nameObj = attributes.get("name");
                    newUser.setName(nameObj != null ? nameObj.toString()
                            : (attributes.get("login") != null ? attributes.get("login").toString() : email));
                    newUser.setProvider(registrationId);
                    newUser.setProviderId(providerId);
                    return userRepository.save(newUser);
                });
        logger.info("User loaded: {}", user.getEmail());

        // Return wrapped user if needed for custom authorities
        return oauth2User;
    }
}
