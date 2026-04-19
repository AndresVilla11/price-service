package com.inditex.price.infrastructure.adapter.exception;

import com.inditex.price.domain.exception.PriceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PriceNotFoundException.class)
    public ProblemDetail handlePriceNotFound(PriceNotFoundException ex,
                                             HttpServletRequest request) {
        log.warn("Price not found: {} — path={}", ex.getMessage(), request.getRequestURI());

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Price Not Found");
        detail.setType(URI.create("https://api.inditex.com/errors/price-not-found"));
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("path", request.getRequestURI());
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex,
                                          HttpServletRequest request) {
        Map<String, String> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        f -> Optional.ofNullable(f.getDefaultMessage()).orElse("invalid"),
                        (a, b) -> a
                ));

        log.warn("Validation failed: {} — path={}", violations, request.getRequestURI());

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        detail.setTitle("Invalid Request");
        detail.setType(URI.create("https://api.inditex.com/errors/validation-error"));
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("path", request.getRequestURI());
        detail.setProperty("violations", violations);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error — path={}", request.getRequestURI(), ex);

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        detail.setTitle("Internal Server Error");
        detail.setType(URI.create("https://api.inditex.com/errors/internal-error"));
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("path", request.getRequestURI());
        return detail;
    }
}