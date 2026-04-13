/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/24/26, 9:31 PM
 *
 */

package com.mutwiri.licensemanager.exceptions;

/**
 * Thrown when license generation or validation fails.
 */
public class LicenseGenerationException extends RuntimeException {
    public LicenseGenerationException(String message) {
        super(message);
    }

    public LicenseGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

