package br.com.fiap.postech.soat16.fase1.dto.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
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
}
