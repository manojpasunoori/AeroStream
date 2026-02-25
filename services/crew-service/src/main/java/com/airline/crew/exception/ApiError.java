package com.airline.crew.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String issue) {}
}
