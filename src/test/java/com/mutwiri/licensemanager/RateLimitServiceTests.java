/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.services.RateLimitService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTests {
    @Test
    void shouldLimitRequestsPerRuntimeKeyPerMinute() {
        RateLimitService rateLimitService = new RateLimitService(2, "", "license-manager:test", true);

        rateLimitService.check("client-a:/validate");
        rateLimitService.check("client-a:/validate");

        assertThatThrownBy(() -> rateLimitService.check("client-a:/validate"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Runtime API rate limit exceeded.");
        assertThatCode(() -> rateLimitService.check("client-b:/validate")).doesNotThrowAnyException();
    }

    @Test
    void shouldLimitRequestsThroughUpstashRestApi() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        Map<String, Integer> counters = new ConcurrentHashMap<>();
        server.createContext("/", exchange -> {
            String[] parts = exchange.getRequestURI().getPath().split("/");
            String body = "{\"result\":1}";
            if (parts.length > 2 && "incr".equals(parts[1])) {
                int count = counters.merge(parts[2], 1, Integer::sum);
                body = "{\"result\":" + count + "}";
            }
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            RateLimitService rateLimitService = new RateLimitService(
                    1, baseUri, "test-token", "license-manager:test", false);

            rateLimitService.check("client-a:/validate");

            assertThatThrownBy(() -> rateLimitService.check("client-a:/validate"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Runtime API rate limit exceeded.");
        } finally {
            server.stop(0);
        }
    }
}
