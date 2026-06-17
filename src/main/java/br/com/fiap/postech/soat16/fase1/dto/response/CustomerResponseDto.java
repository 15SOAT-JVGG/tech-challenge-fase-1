package br.com.fiap.postech.soat16.fase1.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponseDto {

    private UUID customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String document;
    private String documentType;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
