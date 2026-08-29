package br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.panache.common.Sort;

@DisplayName("PageableRequestDto — Unit Tests")
class PageableRequestDtoTest {

    private PageableRequestDto dtoWithSort(List<String> value) throws Exception {
        PageableRequestDto dto = new PageableRequestDto();
        Field field = PageableRequestDto.class.getDeclaredField("sort");
        field.setAccessible(true);
        field.set(dto, value);
        return dto;
    }

    @Nested
    @DisplayName("getSort")
    class GetSort {

        @Test
        @DisplayName("defaults to createdAt descending when sort is null")
        void shouldDefaultWhenNull() throws Exception {
            Sort sort = dtoWithSort(null).getSort();

            assertEquals(1, sort.getColumns().size());
            assertEquals("createdAt", sort.getColumns().getFirst().getName());
            assertEquals(
                    Sort.Direction.Descending,
                    sort.getColumns().getFirst().getDirection());
        }

        @Test
        @DisplayName("defaults to createdAt descending when sort is empty")
        void shouldDefaultWhenEmpty() throws Exception {
            Sort sort = dtoWithSort(List.of()).getSort();

            assertEquals(1, sort.getColumns().size());
            assertEquals("createdAt", sort.getColumns().getFirst().getName());
            assertEquals(
                    Sort.Direction.Descending,
                    sort.getColumns().getFirst().getDirection());
        }

        @Test
        @DisplayName("defaults to ascending when direction is omitted")
        void shouldDefaultToAscendingWhenNoDirection() throws Exception {
            Sort sort = dtoWithSort(List.of("name")).getSort();

            assertEquals("name", sort.getColumns().getFirst().getName());
            assertEquals(
                    Sort.Direction.Ascending,
                    sort.getColumns().getFirst().getDirection());
        }

        @Test
        @DisplayName("sorts descending when direction is desc")
        void shouldSortDescendingWhenDesc() throws Exception {
            Sort sort = dtoWithSort(List.of("name,desc")).getSort();

            assertEquals("name", sort.getColumns().getFirst().getName());
            assertEquals(
                    Sort.Direction.Descending,
                    sort.getColumns().getFirst().getDirection());
        }

        @Test
        @DisplayName("sorts ascending when direction is asc")
        void shouldSortAscendingWhenAsc() throws Exception {
            Sort sort = dtoWithSort(List.of("name,asc")).getSort();

            assertEquals("name", sort.getColumns().getFirst().getName());
            assertEquals(
                    Sort.Direction.Ascending,
                    sort.getColumns().getFirst().getDirection());
        }

        @Test
        @DisplayName("combines multiple sort entries")
        void shouldCombineMultipleEntries() throws Exception {
            Sort sort = dtoWithSort(List.of("name,desc", "age,asc")).getSort();

            assertEquals(2, sort.getColumns().size());
            assertEquals("name", sort.getColumns().get(0).getName());
            assertEquals("age", sort.getColumns().get(1).getName());
        }
    }
}
