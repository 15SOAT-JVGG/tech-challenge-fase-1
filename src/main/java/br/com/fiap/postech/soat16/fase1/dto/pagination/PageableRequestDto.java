package br.com.fiap.postech.soat16.fase1.dto.pagination;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import io.quarkus.panache.common.Sort;
import lombok.Getter;

@Getter
public class PageableRequestDto {

    @QueryParam("q")
    private String q;

    @Min(value = 0, message = "page must be >= 0")
    @QueryParam("page")
    @DefaultValue("0")
    private int page;

    @Min(value = 1, message = "size must be >= 1")
    @Max(value = 100, message = "size must be <= 100")
    @QueryParam("size")
    @DefaultValue("10")
    private int size;

    @Parameter(description = "Sort fields in the format field,direction (e.g. createdAt,desc). Repeatable for multiple fields.")
    @QueryParam("sort")
    private List<String> sort;

    public Sort getSort() {
        if (sort == null || sort.isEmpty()) {
            return Sort.by("createdAt", Sort.Direction.Descending);
        }
        Sort result = null;
        for (String entry : sort) {
            String[] parts = entry.split(",", 2);
            String field = parts[0].trim();
            Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")
                    ? Sort.Direction.Descending
                    : Sort.Direction.Ascending;
            result = result == null ? Sort.by(field, direction) : result.and(field, direction);
        }
        return result;
    }
}
