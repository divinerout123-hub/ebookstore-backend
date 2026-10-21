package com.bookstore.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<T> content) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.getContent().stream().map(mapper).toList());
    }
}
