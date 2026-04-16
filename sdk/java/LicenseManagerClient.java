/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager.sdk;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;

public final class LicenseManagerClient {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final URI baseUri;
    private final String clientToken;
    private final HttpClient httpClient;
    private final int maxAttempts;

    public LicenseManagerClient(URI baseUri, String clientToken) {
        this(baseUri, clientToken, HttpClient.newBuilder().connectTimeout(DEFAULT_TIMEOUT).build(), 3);
    }

    public LicenseManagerClient(URI baseUri, String clientToken, HttpClient httpClient, int maxAttempts) {
        this.baseUri = baseUri;
        this.clientToken = clientToken;
        this.httpClient = httpClient;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public String validate(ValidationRequest request) {
        return post("/api/v1/runtime/licenses/validate", request.toJson());
    }

    public String activate(ActivationRequest request) {
        return post("/api/v1/runtime/licenses/" + path(request.licenseKey()) + "/machines", request.toJson());
    }

    public String heartbeat(HeartbeatRequest request) {
        return post("/api/v1/runtime/licenses/" + path(request.licenseKey()) + "/machines/heartbeat", request.toJson());
    }

    public String checkoutOffline(OfflineCheckoutRequest request) {
        return post("/api/v1/runtime/offline/checkouts", request.toJson());
    }

    public OfflineVerification verifyOffline(String artifact, String publicKeyBase64) {
        String[] parts = artifact == null ? new String[0] : artifact.split("\\.", 2);
        if (parts.length != 2) {
            return new OfflineVerification(false, "MALFORMED_ARTIFACT");
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(payload.getBytes(StandardCharsets.UTF_8));
            boolean valid = verifier.verify(Base64.getUrlDecoder().decode(parts[1]));
            return new OfflineVerification(valid, valid ? payload : "BAD_SIGNATURE");
        } catch (Exception e) {
            return new OfflineVerification(false, "INVALID_ARTIFACT");
        }
    }

    private String post(String path, String body) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                        .timeout(DEFAULT_TIMEOUT)
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + clientToken)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response.body();
                }
                if (response.statusCode() < 500 || attempt == maxAttempts) {
                    throw new LicenseManagerException(response.statusCode(), response.body());
                }
            } catch (IOException e) {
                lastFailure = new LicenseManagerTransportException("Unable to reach License Manager.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LicenseManagerTransportException("Interrupted while contacting License Manager.", e);
            }
            backoff(attempt);
        }
        throw lastFailure == null ? new LicenseManagerTransportException("License Manager request failed.") : lastFailure;
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(Math.min(1_000L, 100L * attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LicenseManagerTransportException("Interrupted while backing off.", e);
        }
    }

    private static String path(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String json(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record ValidationRequest(String licenseKey, String productCode, String policyCode,
                                    String fingerprint, String version) {
        String toJson() {
            return "{\"key\":\"" + json(licenseKey) + "\","
                    + "\"productCode\":\"" + json(productCode) + "\","
                    + "\"policyCode\":\"" + json(policyCode) + "\","
                    + "\"fingerprint\":\"" + json(fingerprint) + "\","
                    + "\"version\":\"" + json(version) + "\"}";
        }
    }

    public record ActivationRequest(String licenseKey, String fingerprint, String name,
                                    String platform, String version) {
        String toJson() {
            return "{\"fingerprint\":\"" + json(fingerprint) + "\","
                    + "\"name\":\"" + json(name) + "\","
                    + "\"platform\":\"" + json(platform) + "\","
                    + "\"version\":\"" + json(version) + "\"}";
        }
    }

    public record HeartbeatRequest(String licenseKey, String fingerprint, String version) {
        String toJson() {
            return "{\"fingerprint\":\"" + json(fingerprint) + "\",\"version\":\"" + json(version) + "\"}";
        }
    }

    public record OfflineCheckoutRequest(String licenseKey, String fingerprint, int ttlDays) {
        String toJson() {
            return "{\"key\":\"" + json(licenseKey) + "\","
                    + "\"fingerprint\":\"" + json(fingerprint) + "\","
                    + "\"ttlDays\":" + Math.max(1, ttlDays) + "}";
        }
    }

    public record OfflineVerification(boolean valid, String detail) {
    }

    public static class LicenseManagerException extends RuntimeException {
        private final int statusCode;

        public LicenseManagerException(int statusCode, String body) {
            super("License Manager returned HTTP " + statusCode + ": " + body);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }

    public static class LicenseManagerTransportException extends RuntimeException {
        public LicenseManagerTransportException(String message) {
            super(message);
        }

        public LicenseManagerTransportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
