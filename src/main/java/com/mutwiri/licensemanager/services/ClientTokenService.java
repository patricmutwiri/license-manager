/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.ClientApiToken;
import com.mutwiri.licensemanager.entities.License;
import com.mutwiri.licensemanager.entities.Product;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;
import com.mutwiri.licensemanager.models.dto.ApiPayloads;
import com.mutwiri.licensemanager.repository.ClientApiTokenRepository;
import com.mutwiri.licensemanager.repository.LicenseRepository;
import com.mutwiri.licensemanager.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
public class ClientTokenService {
    private final SecureRandom secureRandom = new SecureRandom();
    private final ClientApiTokenRepository tokenRepository;
    private final ProductRepository productRepository;
    private final LicenseRepository licenseRepository;
    private final CryptoService cryptoService;
    private final AuditService auditService;

    public ClientTokenService(ClientApiTokenRepository tokenRepository,
            ProductRepository productRepository,
            LicenseRepository licenseRepository,
            CryptoService cryptoService,
            AuditService auditService) {
        this.tokenRepository = tokenRepository;
        this.productRepository = productRepository;
        this.licenseRepository = licenseRepository;
        this.cryptoService = cryptoService;
        this.auditService = auditService;
    }

    @Transactional
    public ApiPayloads.ClientTokenResponse create(ApiPayloads.CreateClientTokenRequest request) {
        String token = generateToken();
        ClientApiToken apiToken = new ClientApiToken();
        apiToken.setName(request.name().trim());
        apiToken.setTokenPrefix(token.substring(0, 16));
        apiToken.setTokenHash(cryptoService.sha256(token));
        apiToken.setExpiresAt(request.expiresAt());
        apiToken.setProduct(request.productId() == null ? null : product(request.productId()));
        apiToken.setLicense(request.licenseId() == null ? null : license(request.licenseId()));
        ClientApiToken saved = tokenRepository.save(apiToken);
        auditService.record("client_token.created", "admin-api", "client_api_token", saved.getId().toString(),
                "Runtime client token created", Map.of("prefix", saved.getTokenPrefix()));
        return new ApiPayloads.ClientTokenResponse(saved.getId(), saved.getName(), saved.getTokenPrefix(), token,
                saved.isActive(), saved.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public ClientApiToken requireRuntimeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ForbiddenException("Valid X-License-Client-Key header is required.");
        }
        ClientApiToken apiToken = tokenRepository.findByTokenHash(cryptoService.sha256(token))
                .orElseThrow(() -> new ForbiddenException("Runtime client token is invalid."));
        if (!apiToken.isActive()) {
            throw new ForbiddenException("Runtime client token is inactive.");
        }
        if (apiToken.getExpiresAt() != null && apiToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("Runtime client token is expired.");
        }
        return apiToken;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "lct_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Product product(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found"));
    }

    private License license(Long id) {
        return licenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("License with ID " + id + " not found"));
    }
}
