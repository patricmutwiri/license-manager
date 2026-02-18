/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 10:34 PM
 *
 */

package com.mutwiri.licensemanager.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Data
@Table(name = "licenses")
public class License {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String key;

    @Column(unique = true, nullable = false)
    private String hostname;

    @Column(unique = true, nullable = false)
    private String applicationName;

    @Column(unique = true, nullable = false)
    private String email;

    @ElementCollection
    @CollectionTable(name = "license_custom_fields", joinColumns = @JoinColumn(name = "license_id"))
    @MapKeyColumn(name = "field_key")
    @Column(name = "field_value")
    private Map<String, String> customFields = new HashMap<>();

    private LocalDateTime expiry;

    private boolean active = true;

    @ManyToOne
    private User user;

    @ManyToOne
    private Organization organization;
}