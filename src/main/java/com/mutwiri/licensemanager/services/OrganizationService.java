/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 12:40 AM
 *
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.Organization;

import java.util.List;
import java.util.Optional;

public interface OrganizationService {
    Organization createOrganization(String name, String domain);

    List<Organization> getAllOrganizations();

    Optional<Organization> getOrganizationById(Long id);

    Optional<Organization> getOrganizationByDomain(String domain);
}
