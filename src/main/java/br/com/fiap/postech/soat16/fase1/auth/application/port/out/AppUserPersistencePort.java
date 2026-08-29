package br.com.fiap.postech.soat16.fase1.auth.application.port.out;

import br.com.fiap.postech.soat16.fase1.auth.domain.model.AppUser;

import io.smallrye.mutiny.Uni;

public interface AppUserPersistencePort {

    Uni<AppUser> findByUsername(String username);

    Uni<Boolean> existsByUsername(String username);

    Uni<AppUser> save(AppUser user);
}
