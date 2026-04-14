/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.entities;

public enum RuntimeTokenScope {
    LICENSE_VALIDATE,
    MACHINE_ACTIVATE,
    MACHINE_HEARTBEAT,
    MACHINE_DEACTIVATE,
    OFFLINE_CHECKOUT,
    OFFLINE_VERIFY,
    OFFLINE_PUBLIC_KEY
}
