package com.techie345.moneys.financial.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import com.techie345.moneys.financial.api.FinancialResourceDtos.ApiError;

@RestControllerAdvice
public class FinancialApiExceptionHandler {
    @ExceptionHandler(VersionConflictException.class)
    ResponseEntity<ApiError> conflict(VersionConflictException exception) {
        return ResponseEntity.status(409).body(new ApiError("VERSION_CONFLICT", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        Map<String,String> fields = new java.util.LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "request validation failed", fields));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    ResponseEntity<ApiError> malformed(Exception e) { return ResponseEntity.badRequest().body(new ApiError("MALFORMED_REQUEST", "request could not be read", Map.of())); }
    @ExceptionHandler({AuthenticationException.class, IllegalStateException.class})
    ResponseEntity<ApiError> unauthenticated(Exception e) { return ResponseEntity.status(401).body(new ApiError("UNAUTHENTICATED", "authentication is required", Map.of())); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(Exception e) { return ResponseEntity.status(403).body(new ApiError("FORBIDDEN", "access denied", Map.of())); }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> optimistic(Exception e) { return ResponseEntity.status(409).body(new ApiError("VERSION_CONFLICT", "resource version is stale", Map.of())); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> domain(Exception e) { return ResponseEntity.badRequest().body(new ApiError("DOMAIN_VALIDATION_ERROR", e.getMessage(), Map.of())); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(Exception e) { return ResponseEntity.badRequest().body(new ApiError("DATA_INTEGRITY_ERROR", "request violates a data constraint", Map.of())); }
}
