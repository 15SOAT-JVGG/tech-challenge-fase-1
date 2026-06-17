package br.com.fiap.postech.soat16.fase1.dto.pagination;

import java.util.ArrayList;
import java.util.List;

public record PageableResponseDto<T>(
        List<T> content,
        PaginationDto pagination
) {

    public PageableResponseDto(List<T> content, PaginationDto pagination) {
        this.content = List.copyOf(content);
        this.pagination = pagination;
    }

    public static <T> PageableResponseDto<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageableResponseDto<>(content, new PaginationDto(
                page, size, totalElements, totalPages,
                page > 0,
                page + 1 < totalPages
        ));
    }

    public static <T> PageableResponseDto<T> emptyList() {
        return new PageableResponseDto<>(new ArrayList<>(), new PaginationDto(0, 0, 0, 0, false, false));
    }
}
