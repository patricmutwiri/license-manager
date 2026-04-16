/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.SchedulerLock;
import com.mutwiri.licensemanager.repository.SchedulerLockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class SchedulerLockService {
    private final SchedulerLockRepository lockRepository;
    private final String owner;

    public SchedulerLockService(SchedulerLockRepository lockRepository,
            @Value("${license.jobs.lock-owner:}") String configuredOwner) {
        this.lockRepository = lockRepository;
        this.owner = configuredOwner == null || configuredOwner.isBlank() ? defaultOwner() : configuredOwner;
    }

    public boolean runWithLock(String name, Duration ttl, Runnable task) {
        return runWithLock(name, ttl, () -> {
            task.run();
            return true;
        }).orElse(false);
    }

    public <T> Optional<T> runWithLock(String name, Duration ttl, Supplier<T> task) {
        if (!tryAcquire(name, ttl)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(task.get());
        } finally {
            release(name);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(String name, Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        Optional<SchedulerLock> existing = lockRepository.findById(name);
        if (existing.isPresent() && existing.get().getLockedUntil().isAfter(now)) {
            return existing.get().getOwner().equals(owner);
        }

        SchedulerLock lock = existing.orElseGet(SchedulerLock::new);
        lock.setName(name);
        lock.setOwner(owner);
        lock.setLockedUntil(now.plus(ttl));
        lockRepository.save(lock);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String name) {
        lockRepository.findById(name)
                .filter(lock -> lock.getOwner().equals(owner))
                .ifPresent(lock -> {
                    lock.setLockedUntil(LocalDateTime.now().minusSeconds(1));
                    lockRepository.save(lock);
                });
    }

    private String defaultOwner() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            hostname = "unknown-host";
        }
        return hostname + ":" + ManagementFactory.getRuntimeMXBean().getName() + ":" + UUID.randomUUID();
    }
}
