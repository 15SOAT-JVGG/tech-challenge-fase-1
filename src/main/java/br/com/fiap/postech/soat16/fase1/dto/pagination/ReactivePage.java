package br.com.fiap.postech.soat16.fase1.dto.pagination;

import java.util.List;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;

/**
 * Monta uma página reativa combinando a consulta dos itens com a contagem total, aplicando o
 * mapeamento entidade → DTO. Centraliza o padrão de paginação reativo compartilhado pelos serviços.
 */
public final class ReactivePage {

    private ReactivePage() {
    }

    public static <E, D> Uni<PageableResponseDto<D>> of(Uni<List<E>> pageUni, Uni<Long> countUni,
            Function<E, D> mapper, int page, int size) {
        return Uni.combine().all().unis(pageUni, countUni).asTuple()
                .map(tuple -> {
                    List<D> data = tuple.getItem1().stream().map(mapper).toList();
                    return PageableResponseDto.of(data, page, size, tuple.getItem2());
                });
    }
}
