/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 1:29 AM
 *
 */

package com.mutwiri.licensemanager.configs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;

@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender(MailProperties props) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        if (props.getHost() != null) {
            sender.setHost(props.getHost());
        }
        if (props.getPort() != null) {
            sender.setPort(props.getPort());
        }
        if (props.getUsername() != null) {
            sender.setUsername(props.getUsername());
        }
        if (props.getPassword() != null) {
            sender.setPassword(props.getPassword());
        }

        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return sender;
    }
}
