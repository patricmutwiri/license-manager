package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.License;

public interface EmailService {
    void sendLicenseBackup(License license);
}
