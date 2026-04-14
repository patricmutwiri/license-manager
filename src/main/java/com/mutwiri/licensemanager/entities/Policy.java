/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "policies")
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    private Product product;

    @Column(unique = true, nullable = false, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LicensingModel licensingModel = LicensingModel.NODE_LOCKED;

    @Column(nullable = false)
    private int maxMachines = 1;

    @Column(nullable = false)
    private int maxSeats = 1;

    @Column(nullable = false)
    private int validityDays = 365;

    @Column(nullable = false)
    private int heartbeatIntervalMinutes = 60;

    @Column(nullable = false)
    private int heartbeatGracePeriodMinutes = 180;

    @Column(nullable = false)
    private int offlineTtlDays = 7;

    private String minVersion;
    private String maxVersion;

    @ElementCollection
    @CollectionTable(name = "policy_entitlements", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "entitlement_code")
    private Set<String> entitlementCodes = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

