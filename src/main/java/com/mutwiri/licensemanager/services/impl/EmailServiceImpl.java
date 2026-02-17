/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 12:36 AM
 *
 */

package com.mutwiri.licensemanager.services.impl;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendLicenseBackup(License license) {
        log.info("Sending license backup email for: {}", license.getKey());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("api@patric.xyz");
            helper.setTo(license.getEmail() != null ? license.getEmail() : "");
            helper.setSubject("License Backup: " + license.getApplicationName());

            StringBuilder body = new StringBuilder();
            body.append("<h1>License Backup Information</h1>");
            body.append("<p><strong>Application:</strong> ").append(license.getApplicationName()).append("</p>");
            body.append("<p><strong>Hostname:</strong> ").append(license.getHostname()).append("</p>");
            body.append("<p><strong>License Key:</strong> <code>").append(license.getKey()).append("</code></p>");
            body.append("<p><strong>Expiry:</strong> ").append(license.getExpiry()).append("</p>");

            if (license.getCustomFields() != null && !license.getCustomFields().isEmpty()) {
                body.append("<h3>Custom Fields:</h3><ul>");
                license.getCustomFields().forEach((k, v) -> body.append("<li><strong>").append(k).append(":</strong> ")
                        .append(v).append("</li>"));
                body.append("</ul>");
            }

            helper.setText(body.toString(), true);
            mailSender.send(message);
            log.info("License backup email sent successfully.");
        } catch (MessagingException e) {
            log.error("Failed to send license backup email", e);
        }
    }
}
