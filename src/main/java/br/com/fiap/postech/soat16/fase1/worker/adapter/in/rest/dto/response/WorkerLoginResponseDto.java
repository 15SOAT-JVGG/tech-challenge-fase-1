package br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkerLoginResponseDto(
        UUID workerId,
        String firstName,
        String lastName,
        String email,
        boolean authenticated
) { }
