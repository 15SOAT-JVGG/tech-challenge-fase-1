package br.com.fiap.postech.soat16.fase1.security;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class AppUserRepository implements PanacheRepository<AppUser> {

    public Uni<AppUser> findByUsername(String username) {
        return find("username = ?1 and active = true", username).firstResult();
    }

    // Checks for duplicates regardless of active status to respect the unique(username) constraint,
    // which also covers deactivated users.
    public Uni<Boolean> existsByUsername(String username) {
        return count("username = ?1", username).map(total -> total > 0);
    }
}
