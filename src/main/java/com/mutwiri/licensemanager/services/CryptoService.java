/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.exceptions.InvalidLicenseRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class CryptoService {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private final byte[] signingSecret;

    public CryptoService(@Value("${license.signing-secret:dev-license-signing-secret-change-me}") String signingSecret) {
        if (signingSecret == null || signingSecret.length() < 24) {
            throw new InvalidLicenseRequestException("license.signing-secret must be at least 24 characters.");
        }
        this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(signingSecret, HMAC_SHA256));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign license artifact", e);
        }
    }

    public boolean verify(String payload, String signature) {
        return constantTimeEquals(sign(payload), signature);
    }

    public String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
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

