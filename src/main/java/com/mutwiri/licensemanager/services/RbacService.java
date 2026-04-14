/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.OrganizationMembership;
import com.mutwiri.licensemanager.entities.Permission;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;
import com.mutwiri.licensemanager.repository.OrganizationMembershipRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RbacService {
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public RbacService(UserRepository userRepository,
            OrganizationMembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public void requireGlobal(Long actorUserId, Permission permission) {
        if (actorUserId == null) {
            return;
        }
        var actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Actor user with ID " + actorUserId + " not found"));
        if (actor.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Actor is not allowed to perform " + permission + ".");
        }
    }

    @Transactional(readOnly = true)
    public void requireOrganization(Long actorUserId, Long organizationId, Permission permission) {
        if (actorUserId == null) {
            return;
        }
        var actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Actor user with ID " + actorUserId + " not found"));
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        OrganizationMembership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
                .orElseThrow(() -> new ForbiddenException("Actor is not a member of this organization."));
        if (!membership.getRole().grants(permission)) {
            throw new ForbiddenException("Actor role " + membership.getRole() + " does not grant " + permission + ".");
        }
    }
}
