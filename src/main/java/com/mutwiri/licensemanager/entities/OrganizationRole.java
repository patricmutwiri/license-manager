/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.entities;

import java.util.EnumSet;
import java.util.Set;

public enum OrganizationRole {
    OWNER(EnumSet.allOf(Permission.class)),
    ADMIN(EnumSet.of(
            Permission.ORGANIZATION_READ,
            Permission.ORGANIZATION_UPDATE,
            Permission.MEMBERSHIP_MANAGE,
            Permission.PRODUCT_MANAGE,
            Permission.POLICY_MANAGE,
            Permission.LICENSE_READ,
            Permission.LICENSE_ISSUE,
            Permission.LICENSE_UPDATE,
            Permission.MACHINE_READ,
            Permission.AUDIT_READ,
            Permission.CLIENT_TOKEN_MANAGE,
            Permission.BILLING_MANAGE)),
    BILLING(EnumSet.of(
            Permission.ORGANIZATION_READ,
            Permission.LICENSE_READ,
            Permission.AUDIT_READ,
            Permission.BILLING_MANAGE)),
    DEVELOPER(EnumSet.of(
            Permission.ORGANIZATION_READ,
            Permission.LICENSE_READ,
            Permission.MACHINE_READ,
            Permission.CLIENT_TOKEN_MANAGE)),
    SUPPORT(EnumSet.of(
            Permission.ORGANIZATION_READ,
            Permission.LICENSE_READ,
            Permission.LICENSE_UPDATE,
            Permission.MACHINE_READ,
            Permission.AUDIT_READ)),
    VIEWER(EnumSet.of(
            Permission.ORGANIZATION_READ,
            Permission.LICENSE_READ,
            Permission.MACHINE_READ,
            Permission.AUDIT_READ));

    private final Set<Permission> permissions;

    OrganizationRole(Set<Permission> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    public boolean grants(Permission permission) {
        return permissions.contains(permission);
    }

    public Set<Permission> permissions() {
        return permissions;
    }
}
