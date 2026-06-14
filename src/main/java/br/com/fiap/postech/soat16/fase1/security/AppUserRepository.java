package br.com.fiap.postech.soat16.fase1.security;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppUserRepository implements PanacheRepository<AppUser> {

    public Uni<AppUser> findByUsername(String username) {
        return find("username = ?1 and active = true", username).firstResult();
    }

    // Verifica duplicidade independente de active para respeitar a constraint unique(username),
    // que abrange também usuários desativados.
    public Uni<Boolean> existsByUsername(String username) {
        return count("username = ?1", username).map(total -> total > 0);
    }
}
