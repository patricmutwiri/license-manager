/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 11:13 PM
 *
 */

package com.mutwiri.licensemanager.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Data
@Table(name = "licenses")
public class License {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "license_key", unique = true, nullable = false)
    private String key;

    private String hostname;

    private String applicationName;

    private String email;

    private String customerName;
    private String customerEmail;

    @ElementCollection
    @CollectionTable(name = "license_custom_fields", joinColumns = @JoinColumn(name = "license_id"))
    @MapKeyColumn(name = "field_key")
    @Column(name = "field_value")
    private Map<String, String> customFields = new HashMap<>();

    private LocalDateTime expiry;

    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LicenseStatus status = LicenseStatus.ACTIVE;

    private LocalDateTime revokedAt;
    private LocalDateTime suspendedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne
    private User user;

    @ManyToOne
    private Organization organization;

    @ManyToOne
    private Product product;

    @ManyToOne
    private Policy policy;
}
