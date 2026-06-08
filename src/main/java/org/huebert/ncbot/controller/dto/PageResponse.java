package org.huebert.ncbot.controller.dto;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Builder
public record PageResponse<T>(List<T> content, int totalPages, int currentPage, long totalElements) {

    public static <T> PageResponse<T> fromPage(Page<T> page) {
        return fromPage(page, Function.identity());
    }

    public static <I, O> PageResponse<O> fromPage(Page<I> page, Function<I, O> converter) {
        return PageResponse.<O>builder()
                .content(page.getContent().stream().map(converter).toList())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .totalElements(page.getTotalElements())
                .build();
    }

}
