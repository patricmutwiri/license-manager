/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.entities;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String domain;

    @OneToMany(mappedBy = "organization")
    private List<License> licenses;
}
