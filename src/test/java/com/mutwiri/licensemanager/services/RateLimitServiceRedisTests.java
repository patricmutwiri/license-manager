/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitServiceRedisTests {

    @Test
    void shouldLimitRequestsThroughRedisAndCloseResources() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> connection = mockConnection(1L, 2L);
        RateLimitService rateLimitService = new RateLimitService(
                1, "license-manager:test", false, client, connection, null, null, null);

        assertThatCode(() -> rateLimitService.check("client-a:/validate")).doesNotThrowAnyException();
        assertThatThrownBy(() -> rateLimitService.check("client-a:/validate"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Runtime API rate limit exceeded.");

        rateLimitService.close();

        verify(connection).close();
        verify(client).shutdown();
    }

    @Test
    void shouldFallbackOrFailClosedWhenRedisIsUnavailable() {
        StatefulRedisConnection<String, String> failOpenConnection = mockConnectionWithFailure();
        RateLimitService failOpen = new RateLimitService(
                1, "license-manager:test", true, null, failOpenConnection, null, null, null);

        failOpen.check("client-a:/validate");
        assertThatThrownBy(() -> failOpen.check("client-a:/validate"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Runtime API rate limit exceeded.");

        StatefulRedisConnection<String, String> failClosedConnection = mockConnectionWithFailure();
        RateLimitService failClosed = new RateLimitService(
                1, "license-manager:test", false, null, failClosedConnection, null, null, null);

        assertThatThrownBy(() -> failClosed.check("client-a:/validate"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Runtime API rate limiter is unavailable.");
    }

    private StatefulRedisConnection<String, String> mockConnection(Long firstCount, Long secondCount) {
        StatefulRedisConnection<String, String> connection = mock();
        RedisCommands<String, String> commands = mock();
        when(connection.sync()).thenReturn(commands);
        when(commands.incr(anyString())).thenReturn(firstCount, secondCount);
        return connection;
    }

    private StatefulRedisConnection<String, String> mockConnectionWithFailure() {
        StatefulRedisConnection<String, String> connection = mock();
        RedisCommands<String, String> commands = mock();
        when(connection.sync()).thenReturn(commands);
        when(commands.incr(anyString())).thenThrow(new IllegalStateException("redis down"));
        return connection;
    }
}
