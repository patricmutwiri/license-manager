package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.License;

/**
 * Service for sending emails related to licenses.
 */
public interface EmailService {
    /**
     * Send a backup email for the generated license (synchronous, logs errors).
     */
    void sendLicenseBackup(License license);

    /**
     * Send a backup email asynchronously. Errors are logged but not thrown.
     */
    void sendLicenseBackupAsync(License license);
}
