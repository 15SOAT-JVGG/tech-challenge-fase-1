package br.com.fiap.postech.soat16.fase1.auth.adapter.in.rest.mapper;

import br.com.fiap.postech.soat16.fase1.auth.adapter.in.rest.dto.request.LoginRequestDto;
import br.com.fiap.postech.soat16.fase1.auth.adapter.in.rest.dto.response.LoginResponseDto;
import br.com.fiap.postech.soat16.fase1.auth.application.command.LoginCommand;
import br.com.fiap.postech.soat16.fase1.auth.application.result.LoginResult;

public final class AuthRestMapper {

    private AuthRestMapper() {
    }

    public static LoginCommand toCommand(LoginRequestDto request) {
        return new LoginCommand(request.username(), request.password());
    }

    public static LoginResponseDto toResponse(LoginResult result) {
        return new LoginResponseDto(
                result.token(),
                result.username(),
                result.role(),
                result.expiresIn());
    }
}
