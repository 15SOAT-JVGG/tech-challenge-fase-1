package br.com.fiap.postech.soat16.fase1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCreateRequestDto {

    @NotBlank(message = "firstName cannot be blank")
    private String firstName;

    @NotBlank(message = "lastName cannot be blank")
    private String lastName;

    @NotBlank(message = "email cannot be blank")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "phoneNumber cannot be blank")
    private String phoneNumber;

    @NotBlank(message = "document cannot be blank")
    private String document;
}
