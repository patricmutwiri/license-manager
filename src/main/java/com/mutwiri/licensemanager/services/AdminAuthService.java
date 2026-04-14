/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {
    private final String adminApiKey;

    public AdminAuthService(@Value("${license.admin.api-key:dev-admin-key}") String adminApiKey) {
        this.adminApiKey = adminApiKey;
    }

    public void requireAdmin(String providedApiKey) {
        if (providedApiKey == null || providedApiKey.isBlank() || !constantTimeEquals(adminApiKey, providedApiKey)) {
            throw new ForbiddenException("Valid X-Admin-Api-Key header is required.");
        }
    }

    private boolean constantTimeEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        int diff = expected.length() ^ provided.length();
        int max = Math.max(expected.length(), provided.length());
        for (int i = 0; i < max; i++) {
            char left = i < expected.length() ? expected.charAt(i) : 0;
            char right = i < provided.length() ? provided.charAt(i) : 0;
            diff |= left ^ right;
        }
        return diff == 0;
    }
}

