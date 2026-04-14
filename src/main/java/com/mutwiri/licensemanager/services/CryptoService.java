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

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class CryptoService {
    private static final String ED25519 = "Ed25519";
    private final PrivateKey offlinePrivateKey;
    private final PublicKey offlinePublicKey;

    public CryptoService(
            @Value("${license.offline.private-key-base64:}") String privateKeyBase64,
            @Value("${license.offline.public-key-base64:}") String publicKeyBase64) {
        try {
            if (privateKeyBase64 != null && !privateKeyBase64.isBlank()) {
                this.offlinePrivateKey = loadPrivateKey(privateKeyBase64);
                this.offlinePublicKey = publicKeyBase64 == null || publicKeyBase64.isBlank()
                        ? null
                        : loadPublicKey(publicKeyBase64);
            } else {
                KeyPairGenerator generator = KeyPairGenerator.getInstance(ED25519);
                KeyPair keyPair = generator.generateKeyPair();
                this.offlinePrivateKey = keyPair.getPrivate();
                this.offlinePublicKey = keyPair.getPublic();
            }
            if (this.offlinePublicKey == null) {
                throw new InvalidLicenseRequestException("license.offline.public-key-base64 is required when a private key is configured.");
            }
        } catch (InvalidLicenseRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidLicenseRequestException("Offline signing key configuration is invalid.");
        }
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
            Signature signature = Signature.getInstance(ED25519);
            signature.initSign(offlinePrivateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign license artifact", e);
        }
    }

    public boolean verify(String payload, String signature) {
        try {
            Signature verifier = Signature.getInstance(ED25519);
            verifier.initVerify(offlinePublicKey);
            verifier.update(payload.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getUrlDecoder().decode(signature));
        } catch (Exception e) {
            return false;
        }
    }

    public String offlinePublicKeyBase64() {
        return Base64.getEncoder().encodeToString(offlinePublicKey.getEncoded());
    }

    public String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private PrivateKey loadPrivateKey(String privateKeyBase64) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(privateKeyBase64);
        return KeyFactory.getInstance(ED25519).generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey loadPublicKey(String publicKeyBase64) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(publicKeyBase64);
        return KeyFactory.getInstance(ED25519).generatePublic(new X509EncodedKeySpec(decoded));
    }
}
