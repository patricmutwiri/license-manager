/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.services.CryptoService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoServiceTests {
    @Test
    void shouldSignAndVerifyOfflinePayloadsWithEd25519() {
        CryptoService cryptoService = new CryptoService("", "");
        String payload = "{\"license\":\"lic_test\"}";

        String signature = cryptoService.sign(payload);

        assertThat(signature).isNotBlank();
        assertThat(cryptoService.verify(payload, signature)).isTrue();
        assertThat(cryptoService.verify("{\"license\":\"tampered\"}", signature)).isFalse();
        assertThat(cryptoService.offlinePublicKeyBase64()).isNotBlank();
    }
}
