/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.Organization;
import com.mutwiri.licensemanager.entities.OrganizationMembership;
import com.mutwiri.licensemanager.entities.OrganizationRole;
import com.mutwiri.licensemanager.entities.Permission;
import com.mutwiri.licensemanager.entities.User;
import com.mutwiri.licensemanager.entities.UserRole;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;
import com.mutwiri.licensemanager.repository.OrganizationMembershipRepository;
import com.mutwiri.licensemanager.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RbacServiceTests {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OrganizationMembershipRepository membershipRepository = mock(OrganizationMembershipRepository.class);
    private final RbacService rbacService = new RbacService(userRepository, membershipRepository);

    @Test
    void shouldAllowSystemAndAdminActorsGloballyAndForOrganizations() {
        User admin = user(UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThat(rbacService.hasGlobal(null, Permission.PRODUCT_MANAGE)).isTrue();
        assertThat(rbacService.hasOrganization(null, 10L, Permission.PRODUCT_MANAGE)).isTrue();
        assertThat(rbacService.hasGlobal(1L, Permission.PRODUCT_MANAGE)).isTrue();
        assertThat(rbacService.hasOrganization(1L, 10L, Permission.PRODUCT_MANAGE)).isTrue();
        assertThatCode(() -> rbacService.requireGlobal(1L, Permission.PRODUCT_MANAGE)).doesNotThrowAnyException();
        assertThatCode(() -> rbacService.requireOrganization(1L, 10L, Permission.PRODUCT_MANAGE)).doesNotThrowAnyException();
    }

    @Test
    void shouldEnforceMembershipPermissionMatrixAndMissingActors() {
        User customer = user(UserRole.CUSTOMER);
        OrganizationMembership viewerMembership = membership(OrganizationRole.VIEWER);
        OrganizationMembership ownerMembership = membership(OrganizationRole.OWNER);

        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(userRepository.findById(404L)).thenReturn(Optional.empty());
        when(membershipRepository.findByOrganizationIdAndUserId(10L, 2L)).thenReturn(Optional.of(viewerMembership));
        when(membershipRepository.findByOrganizationIdAndUserId(11L, 2L)).thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.findByOrganizationIdAndUserId(12L, 2L)).thenReturn(Optional.empty());

        assertThat(rbacService.hasGlobal(2L, Permission.PRODUCT_MANAGE)).isFalse();
        assertThat(rbacService.hasOrganization(2L, 10L, Permission.LICENSE_READ)).isTrue();
        assertThat(rbacService.hasOrganization(2L, 11L, Permission.PRODUCT_MANAGE)).isTrue();
        assertThat(rbacService.hasOrganization(2L, 12L, Permission.LICENSE_READ)).isFalse();
        assertThatThrownBy(() -> rbacService.requireGlobal(2L, Permission.PRODUCT_MANAGE))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> rbacService.requireOrganization(2L, 12L, Permission.LICENSE_READ))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> rbacService.hasGlobal(404L, Permission.PRODUCT_MANAGE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User user(UserRole role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    private OrganizationMembership membership(OrganizationRole role) {
        Organization organization = new Organization();
        organization.setId(10L);
        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(organization);
        membership.setRole(role);
        return membership;
    }
}
