/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 12:51 AM
 *
 */

package com.mutwiri.licensemanager.services.impl;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendLicenseBackup(License license) {
        log.info("Sending license backup email for: {}", license.getKey());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("api@patric.xyz");
            helper.setTo(license.getEmail() != null ? license.getEmail() : "");
            helper.setSubject("License Backup: " + license.getApplicationName());

            String htmlBody = buildEmailBody(license);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("License backup email sent successfully to: {}", license.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send license backup email to: {}", license.getEmail(), e);
        }
    }

    @Override
    @Async("asyncExecutor")
    public void sendLicenseBackupAsync(License license) {
        // Delegating to sync method to avoid code duplication
        // The @Async annotation ensures this runs in a separate thread pool
        sendLicenseBackup(license);
    }

    /**
     * Build HTML email body with license details.
     */
    private String buildEmailBody(License license) {
        StringBuilder body = new StringBuilder();
        body.append("<h1>License Backup Information</h1>");
        body.append("<p><strong>Application:</strong> ").append(escapeHtml(license.getApplicationName())).append("</p>");
        body.append("<p><strong>Hostname:</strong> ").append(escapeHtml(license.getHostname())).append("</p>");
        body.append("<p><strong>License Key:</strong> <code>").append(escapeHtml(license.getKey())).append("</code></p>");
        body.append("<p><strong>Expiry:</strong> ").append(license.getExpiry()).append("</p>");

        if (license.getCustomFields() != null && !license.getCustomFields().isEmpty()) {
            body.append("<h3>Custom Fields:</h3><ul>");
            license.getCustomFields().forEach((k, v) -> body
                    .append("<li><strong>").append(escapeHtml(k)).append(":</strong> ")
                    .append(escapeHtml(v)).append("</li>"));
            body.append("</ul>");
        }

        return body.toString();
    }

    /**
     * Simple HTML escaping to prevent injection.
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
