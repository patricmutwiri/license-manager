/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.sdk;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class LicenseManagerClient {
    private final URI baseUri;
    private final String clientToken;
    private final HttpClient httpClient;

    public LicenseManagerClient(URI baseUri, String clientToken) {
        this.baseUri = baseUri;
        this.clientToken = clientToken;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String validate(String licenseKey, String productCode, String fingerprint)
            throws IOException, InterruptedException {
        return post("/api/v1/runtime/licenses/validate",
                "{\"key\":\"" + escape(licenseKey) + "\","
                        + "\"productCode\":\"" + escape(productCode) + "\","
                        + "\"fingerprint\":\"" + escape(fingerprint) + "\"}");
    }

    public String activate(String licenseKey, String fingerprint, String name, String platform, String version)
            throws IOException, InterruptedException {
        return post("/api/v1/runtime/licenses/" + escapePath(licenseKey) + "/machines",
                "{\"fingerprint\":\"" + escape(fingerprint) + "\","
                        + "\"name\":\"" + escape(name) + "\","
                        + "\"platform\":\"" + escape(platform) + "\","
                        + "\"version\":\"" + escape(version) + "\"}");
    }

    public String heartbeat(String licenseKey, String fingerprint, String version)
            throws IOException, InterruptedException {
        return post("/api/v1/runtime/licenses/" + escapePath(licenseKey) + "/machines/heartbeat",
                "{\"fingerprint\":\"" + escape(fingerprint) + "\","
                        + "\"version\":\"" + escape(version) + "\"}");
    }

    public String checkoutOffline(String licenseKey, String fingerprint, int ttlDays)
            throws IOException, InterruptedException {
        return post("/api/v1/runtime/offline/checkouts",
                "{\"key\":\"" + escape(licenseKey) + "\","
                        + "\"fingerprint\":\"" + escape(fingerprint) + "\","
                        + "\"ttlDays\":" + ttlDays + "}");
    }

    private String post(String path, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + clientToken)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("License Manager returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String escapePath(String value) {
        return escape(value).replace("/", "%2F");
    }
}
