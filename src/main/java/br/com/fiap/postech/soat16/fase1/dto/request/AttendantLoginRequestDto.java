package br.com.fiap.postech.soat16.fase1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendantLoginRequestDto {

    @NotBlank(message = "email cannot be blank")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "password cannot be blank")
    private String password;
}
