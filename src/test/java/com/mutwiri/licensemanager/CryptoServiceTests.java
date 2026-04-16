/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.services.CryptoService;
import com.mutwiri.licensemanager.exceptions.InvalidLicenseRequestException;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoServiceTests {
    @Test
    void shouldSignAndVerifyOfflinePayloadsWithEd25519() {
        CryptoService cryptoService = new CryptoService("", "");
        String payload = "{\"license\":\"lic_test\"}";

        String signature = cryptoService.sign(payload);

        assertThat(signature).isNotBlank();
        assertThat(cryptoService.verify(payload, signature)).isTrue();
        assertThat(cryptoService.verify("{\"license\":\"tampered\"}", signature)).isFalse();
        assertThat(cryptoService.verify(payload, "not-base64")).isFalse();
        assertThat(cryptoService.offlinePublicKeyBase64()).isNotBlank();
        assertThat(cryptoService.decode(cryptoService.encode(payload))).isEqualTo(payload);
        assertThat(cryptoService.sha256("value")).hasSize(64);
    }

    @Test
    void shouldUseConfiguredOfflineKeyPairAndRejectInvalidConfiguration() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair keyPair = generator.generateKeyPair();
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        CryptoService cryptoService = new CryptoService(privateKey, publicKey);

        String signature = cryptoService.sign("payload");

        assertThat(cryptoService.verify("payload", signature)).isTrue();
        assertThat(cryptoService.offlinePublicKeyBase64()).isEqualTo(publicKey);
        assertThatThrownBy(() -> new CryptoService(privateKey, ""))
                .isInstanceOf(InvalidLicenseRequestException.class)
                .hasMessage("license.offline.public-key-base64 is required when a private key is configured.");
        assertThatThrownBy(() -> new CryptoService("invalid", "invalid"))
                .isInstanceOf(InvalidLicenseRequestException.class)
                .hasMessage("Offline signing key configuration is invalid.");
    }
}
