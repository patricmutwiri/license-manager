/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.entities.SchedulerLock;
import com.mutwiri.licensemanager.repository.SchedulerLockRepository;
import com.mutwiri.licensemanager.services.EmailService;
import com.mutwiri.licensemanager.services.SchedulerLockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SchedulerLockServiceTests {
    @Autowired
    private SchedulerLockRepository lockRepository;

    @Autowired
    private SchedulerLockService lockService;

    @MockitoBean
    private EmailService emailService;

    @Test
    void shouldRunTaskAndReleaseLock() {
        AtomicInteger executions = new AtomicInteger();

        boolean ran = lockService.runWithLock("test-job", Duration.ofSeconds(60), () -> {
            executions.incrementAndGet();
        });

        SchedulerLock lock = lockRepository.findById("test-job").orElseThrow();
        assertThat(ran).isTrue();
        assertThat(executions).hasValue(1);
        assertThat(lock.getLockedUntil()).isBefore(LocalDateTime.now());
    }

    @Test
    void shouldSkipWhenAnotherOwnerHasActiveLease() {
        SchedulerLockService otherOwner = new SchedulerLockService(lockRepository, "other-owner");
        assertThat(otherOwner.tryAcquire("contended-job", Duration.ofMinutes(5))).isTrue();

        Optional<String> result = lockService.runWithLock("contended-job", Duration.ofSeconds(30), () -> "ran");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldTakeOverExpiredLease() {
        SchedulerLockService otherOwner = new SchedulerLockService(lockRepository, "expired-owner");
        assertThat(otherOwner.tryAcquire("expired-job", Duration.ofSeconds(30))).isTrue();
        SchedulerLock staleLock = lockRepository.findById("expired-job").orElseThrow();
        staleLock.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        lockRepository.save(staleLock);

        Optional<String> result = lockService.runWithLock("expired-job", Duration.ofSeconds(30), () -> "ran");

        assertThat(result).contains("ran");
    }
}
