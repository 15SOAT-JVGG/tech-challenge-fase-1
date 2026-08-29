package br.com.fiap.postech.soat16.fase1.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import br.com.fiap.postech.soat16.fase1.service.PasswordService;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository userRepository;
    private final PasswordService passwordService;

    public DataSeeder(AppUserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @ConfigProperty(name = "app.seed.admin-username", defaultValue = "admin")
    String adminUsername;

    @ConfigProperty(name = "app.seed.admin-password")
    Optional<String> adminPassword;

    @ConfigProperty(name = "app.seed.mechanic-username", defaultValue = "mecanico")
    String mechanicUsername;

    @ConfigProperty(name = "app.seed.mechanic-password")
    Optional<String> mechanicPassword;

    @ConfigProperty(name = "app.seed.enabled", defaultValue = "true")
    boolean seedEnabled;

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    void onStart(@Observes StartupEvent ev) {
        if (!seedEnabled) {
            LOG.info("Initial seed disabled (app.seed.enabled=false).");
            return;
        }
        try {
            // O Hibernate Reactive exige contexto Vert.x e transação reativa; a inicialização
            // aguarda a conclusão da carga.
            VertxContextSupport.subscribeAndAwait(() ->
                Panache.withTransaction(() ->
                    seedUser(adminUsername, adminPassword, "ADMIN")
                        .chain(() -> seedUser(mechanicUsername, mechanicPassword, "MECHANIC"))));
        } catch (Throwable e) {
            // subscribeAndAwait declara Throwable, portanto este catch não pode ser mais específico.
            throw new IllegalStateException("Failed to run initial seed.", e);
        }
    }

    private Uni<Void> seedUser(String username, Optional<String> configuredPassword, String role) {
        return userRepository.findByUsername(username)
            .flatMap(existing -> {
                if (existing != null) {
                    return Uni.createFrom().voidItem();
                }
                String password = configuredPassword.filter(p -> !p.isBlank()).orElseGet(() -> {
                    String generated = generateRandomPassword();
                    LOG.warnf("[SEED] Password not configured for '%s'. Generated password (note it down and change it): %s",
                        username, generated);
                    return generated;
                });

                AppUser user = new AppUser(username, passwordService.hash(password), role);
                return userRepository.persist(user)
                    .invoke(() -> LOG.infof("Initial user created: %s (%s)", username, role))
                    .replaceWithVoid();
            });
    }

    private static String generateRandomPassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
