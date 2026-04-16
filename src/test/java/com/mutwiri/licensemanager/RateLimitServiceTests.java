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

    @Test
    void shouldParseStringCountsFromUpstashAndFailClosedWhenUnavailable() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] response = "{\"result\":\"1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            RateLimitService rateLimitService = new RateLimitService(
                    2, baseUri, "test-token", "license-manager:test", false);

            assertThatCode(() -> rateLimitService.check("client-a:/validate")).doesNotThrowAnyException();
        } finally {
            server.stop(0);
        }

        RateLimitService failClosed = new RateLimitService(
                2, URI.create("http://127.0.0.1:1"), "test-token", "license-manager:test", false);
        assertThatThrownBy(() -> failClosed.check("client-a:/validate"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Runtime API rate limiter is unavailable.");
    }

    @Test
    void shouldFallbackToLocalLimiterWhenUpstashFailsOpen() {
        RateLimitService rateLimitService = new RateLimitService(
                1, URI.create("http://127.0.0.1:1"), "test-token", "license-manager:test", true);

        rateLimitService.check("client-a:/validate");

        assertThatThrownBy(() -> rateLimitService.check("client-a:/validate"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Runtime API rate limit exceeded.");
    }

    @Test
    void shouldRejectMalformedUpstashRedisUrl() {
        assertThatThrownBy(() -> new RateLimitService(
                1, "redis://example.upstash.io:6379", "license-manager:test", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Upstash Redis URL must include a password token.");
        assertThatCode(() -> new RateLimitService(
                1, "redis://user:test-token@example.upstash.io:6379", "license-manager:test", true)
                .close()).doesNotThrowAnyException();
    }

    @Test
    void shouldHandleInvalidUpstashResponsesAccordingToFailMode() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/nonnumeric", exchange -> {
            byte[] response = "{\"result\":{}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/http500", exchange -> {
            byte[] response = "{\"error\":\"down\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

            assertThatThrownBy(() -> new RateLimitService(
                    1, baseUri.resolve("/nonnumeric/"), "test-token", "license-manager:test", false)
                    .check("client-a:/validate"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Runtime API rate limiter is unavailable.");
            RateLimitService failOpen = new RateLimitService(
                    1, baseUri.resolve("/http500/"), "test-token", "license-manager:test", true);
            failOpen.check("client-a:/validate");
            assertThatThrownBy(() -> failOpen.check("client-a:/validate"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Runtime API rate limit exceeded.");
        } finally {
            server.stop(0);
        }
    }
}
