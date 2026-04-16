/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/18/26, 12:40 AM
 *
 */

package com.mutwiri.licensemanager.controllers;

import java.nio.file.AccessDeniedException;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.mutwiri.licensemanager.exceptions.ConflictException;
import com.mutwiri.licensemanager.exceptions.ErrorResponse;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.exceptions.InvalidLicenseRequestException;
import com.mutwiri.licensemanager.exceptions.LicenseGenerationException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for consistent error responses across the application.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle custom ResourceNotFoundException.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidLicenseRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleInvalidLicenseRequest(InvalidLicenseRequestException ex, HttpServletRequest request) {
        log.warn("Invalid license request: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "INVALID_LICENSE_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        log.warn("Conflict: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("Forbidden request: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), request);
    }

    /**
     * Handle custom LicenseGenerationException.
     */
    @ExceptionHandler(LicenseGenerationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleLicenseGenerationError(LicenseGenerationException ex, HttpServletRequest request) {
        log.error("License generation failed: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "LICENSE_ERROR", ex.getMessage(), request);
    }

    /**
     * Handle JPA EntityNotFoundException.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entity not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource could not be found.", request);
    }

    /**
     * Handle validation errors from @Valid.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Validation failed";
        log.warn("Validation error: {}", message);
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    /**
     * Handle access denied errors.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to access this resource.", request);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                code,
                message,
                status.value(),
                request == null ? null : request.getRequestURI(),
                MDC.get("requestId"),
                System.currentTimeMillis()));
    }
}
