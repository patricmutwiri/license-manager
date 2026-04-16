/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "scheduler_locks")
public class SchedulerLock {
    @Id
    @Column(length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String owner;

    @Column(nullable = false)
    private LocalDateTime lockedUntil;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
