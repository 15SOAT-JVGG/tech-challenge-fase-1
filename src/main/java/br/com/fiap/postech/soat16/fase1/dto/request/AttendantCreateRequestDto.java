package br.com.fiap.postech.soat16.fase1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendantCreateRequestDto {

    @NotBlank(message = "firstName cannot be blank")
    private String firstName;

    @NotBlank(message = "lastName cannot be blank")
    private String lastName;

    @NotBlank(message = "email cannot be blank")
    @Email(message = "email must be a valid email address")
    private String email;

    private String phoneNumber;

    @NotBlank(message = "password cannot be blank")
    @Size(min = 8, message = "password must have at least 8 characters")
    private String password;
}
