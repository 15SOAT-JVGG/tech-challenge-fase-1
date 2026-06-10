package br.com.fiap.postech.soat16.fase1.dto.pagination;

import java.util.List;

public record PageableResponse<T>(
        List<T> content,
        Pagination pagination
) {

    public PageableResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    @Override
    public List<T> content() {
        return List.copyOf(content);
    }

    public static <T> PageableResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageableResponse<>(content, new Pagination(
                page, size, totalElements, totalPages,
                page > 0,
                page + 1 < totalPages
        ));
    }

    public static <T> PageableResponse<T> emptyList() {
        return new PageableResponse<>(List.of(), new Pagination(0, 0, 0, 0, false, false));
    }
}
