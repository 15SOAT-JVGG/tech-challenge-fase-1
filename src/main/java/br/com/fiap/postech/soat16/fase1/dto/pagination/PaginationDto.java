package br.com.fiap.postech.soat16.fase1.dto.pagination;

public record PaginationDto(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) { }
