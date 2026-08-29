package br.com.fiap.postech.soat16.fase1.shared.application.result;

import java.util.List;

public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {

    public PagedResult {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PagedResult<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page > 0,
                page + 1 < totalPages);
    }
}
