/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/9/26, 10:23 PM
 *
 */

package com.mutwiri.licensemanager.services.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.repository.OrganizationRepository;
import com.mutwiri.licensemanager.services.OrganizationService;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public Organization createOrganization(String name, String domain) {
        Organization org = new Organization();
        org.setName(name);
        org.setDomain(domain);
        return organizationRepository.save(org);
    }

    @Override
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Override
    public Optional<Organization> getOrganizationById(Long id) {
        return organizationRepository.findById(id);
    }

    @Override
    public Optional<Organization> getOrganizationByDomain(String domain) {
        return organizationRepository.findByDomain(domain);
    }
}
