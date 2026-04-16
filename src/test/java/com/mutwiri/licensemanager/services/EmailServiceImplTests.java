/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.services.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceImplTests {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void shouldSkipEmailWhenDisabled() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, false, "noreply@example.com");

        service.sendLicenseBackup(license("<App>", "host", "customer@example.com"));

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void shouldSendEscapedLicenseBackupEmail() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        EmailServiceImpl service = new EmailServiceImpl(mailSender, true, "noreply@example.com");

        service.sendLicenseBackup(license("<App>", "<host>", "customer@example.com"));

        verify(mailSender).send(message);
    }

    @Test
    void shouldSwallowMailFailuresAndAsyncDelegateToSyncPath() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        org.mockito.Mockito.doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));
        EmailServiceImpl service = new EmailServiceImpl(mailSender, true, "noreply@example.com");

        service.sendLicenseBackupAsync(license("App", null, "customer@example.com"));

        verify(mailSender).send(message);
    }

    private License license(String applicationName, String hostname, String email) {
        License license = new License();
        license.setKey("lic_test");
        license.setApplicationName(applicationName);
        license.setHostname(hostname);
        license.setEmail(email);
        license.setExpiry(LocalDateTime.now().plusDays(30));
        license.setCustomFields(Map.of("danger", "<script>"));
        return license;
    }
}
