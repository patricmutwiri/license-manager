/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.services;

import java.util.List;
import java.util.Optional;

import com.mutwiri.licensemanager.entities.Organization;

public interface OrganizationService {
    Organization createOrganization(String name, String domain);

    List<Organization> getAllOrganizations();

    Optional<Organization> getOrganizationById(Long id);

    Optional<Organization> getOrganizationByDomain(String domain);
}
