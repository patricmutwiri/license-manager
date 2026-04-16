/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

package com.mutwiri.licensemanager;

import com.mutwiri.licensemanager.controllers.GlobalExceptionHandler;
import com.mutwiri.licensemanager.exceptions.ConflictException;
import com.mutwiri.licensemanager.exceptions.ErrorResponse;
import com.mutwiri.licensemanager.exceptions.ForbiddenException;
import com.mutwiri.licensemanager.exceptions.InvalidLicenseRequestException;
import com.mutwiri.licensemanager.exceptions.LicenseGenerationException;
import com.mutwiri.licensemanager.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.nio.file.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTests {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldReturnStructuredErrorsForKnownExceptions() {
        MDC.put("requestId", "req-123");

        assertError(handler.handleResourceNotFound(new ResourceNotFoundException("missing"), request),
                HttpStatus.NOT_FOUND, "NOT_FOUND", "missing");
        assertError(handler.handleInvalidLicenseRequest(new InvalidLicenseRequestException("bad"), request),
                HttpStatus.BAD_REQUEST, "INVALID_LICENSE_REQUEST", "bad");
        assertError(handler.handleConflict(new ConflictException("duplicate"), request),
                HttpStatus.CONFLICT, "CONFLICT", "duplicate");
        assertError(handler.handleForbidden(new ForbiddenException("nope"), request),
                HttpStatus.FORBIDDEN, "FORBIDDEN", "nope");
    }

    @Test
    void shouldReturnStructuredErrorsForFrameworkAndUnexpectedExceptions() {
        assertError(handler.handleLicenseGenerationError(new LicenseGenerationException("failed"), request),
                HttpStatus.INTERNAL_SERVER_ERROR, "LICENSE_ERROR", "failed");
        assertError(handler.handleEntityNotFound(new EntityNotFoundException("entity"), request),
                HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource could not be found.");
        assertError(handler.handleAccessDenied(new AccessDeniedException("denied"), request),
                HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to access this resource.");
        assertError(handler.handleGeneralException(new IllegalStateException("boom"), request),
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred. Please try again later.");
    }

    @Test
    void shouldReturnStructuredValidationErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(new FieldError("request", "name", "Name is required"));

        assertError(handler.handleValidationError(exception, request),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Name is required");

        when(bindingResult.getFieldError()).thenReturn(null);
        assertError(handler.handleValidationError(exception, request),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed");
    }

    private void assertError(ResponseEntity<ErrorResponse> response, HttpStatus status, String code, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(status.value());
        assertThat(response.getBody().getCode()).isEqualTo(code);
        assertThat(response.getBody().getMessage()).isEqualTo(message);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }
}
