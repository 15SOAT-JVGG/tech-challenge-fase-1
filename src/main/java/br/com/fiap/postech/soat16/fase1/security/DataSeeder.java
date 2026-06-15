package br.com.fiap.postech.soat16.fase1.security;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@ApplicationScoped
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    AppUserRepository userRepository;

    public DataSeeder(AppUserRepository userRepository) {
        this.userRepository = userRepository;
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

    void onStart(@Observes StartupEvent ev) {
        if (!seedEnabled) {
            LOG.info("Seed inicial desabilitado (app.seed.enabled=false).");
            return;
        }
        try {
            // Hibernate Reactive exige contexto Vert.x e sessão/transação reativos;
            // subscribeAndAwait bloqueia o startup até o seed concluir.
            VertxContextSupport.subscribeAndAwait(() ->
                Panache.withTransaction(() ->
                    seedUser(adminUsername, adminPassword, "ADMIN")
                        .chain(() -> seedUser(mechanicUsername, mechanicPassword, "MECHANIC"))));
        } catch (Throwable e) {
            throw new IllegalStateException("Falha ao executar seed inicial.", e);
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
                    LOG.warnf("[SEED] Senha não configurada para '%s'. Senha gerada (anote e altere): %s",
                        username, generated);
                    return generated;
                });

                AppUser user = new AppUser(username, BcryptUtil.bcryptHash(password), role);
                return userRepository.persist(user)
                    .invoke(() -> LOG.infof("Usuário inicial criado: %s (%s)", username, role))
                    .replaceWithVoid();
            });
    }

    private static String generateRandomPassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
