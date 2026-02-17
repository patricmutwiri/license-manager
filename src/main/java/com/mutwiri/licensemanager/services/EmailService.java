/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 12:40 AM
 *
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.License;

public interface EmailService {
    void sendLicenseBackup(License license);
}
