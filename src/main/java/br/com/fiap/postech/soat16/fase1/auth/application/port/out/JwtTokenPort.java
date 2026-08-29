package br.com.fiap.postech.soat16.fase1.auth.application.port.out;

import br.com.fiap.postech.soat16.fase1.auth.application.result.IssuedToken;

@FunctionalInterface
public interface JwtTokenPort {

    IssuedToken issue(String subject, String role);
}
