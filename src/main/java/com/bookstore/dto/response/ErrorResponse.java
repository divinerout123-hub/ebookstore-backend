package com.bookstore.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldDetail> details) {

    public record FieldDetail(String field, String message) {
    }
}
