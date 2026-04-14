/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.exceptions;

public class InvalidLicenseRequestException extends RuntimeException {
    public InvalidLicenseRequestException(String message) {
        super(message);
    }
}

