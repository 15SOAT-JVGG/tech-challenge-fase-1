package br.com.fiap.postech.soat16.fase1.auth.adapter.in.startup;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import br.com.fiap.postech.soat16.fase1.auth.application.AuthService;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthService authService;

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

    public DataSeeder(AuthService authService) {
        this.authService = authService;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    void onStart(@Observes StartupEvent event) {
        if (!seedEnabled) {
            LOG.info("Initial seed disabled (app.seed.enabled=false).");
            return;
        }
        try {
            VertxContextSupport.subscribeAndAwait(() ->
                Panache.withTransaction(() ->
                    seedUser(adminUsername, adminPassword, "ADMIN")
                        .chain(() -> seedUser(mechanicUsername, mechanicPassword, "MECHANIC"))));
        } catch (Throwable exception) {
            throw new IllegalStateException("Failed to run initial seed.", exception);
        }
    }

    private Uni<Void> seedUser(String username, Optional<String> configuredPassword, String role) {
        return authService.hasActiveUser(username)
                .flatMap(existing -> {
                    if (Boolean.TRUE.equals(existing)) {
                        return Uni.createFrom().voidItem();
                    }
                    String password = configuredPassword
                            .filter(value -> !value.isBlank())
                            .orElseGet(() -> generatedPasswordFor(username));
                    return authService.createUser(username, password, role)
                            .invoke(() -> LOG.infof("Initial user created: %s (%s)", username, role));
                });
    }

    private static String generatedPasswordFor(String username) {
        String generated = generateRandomPassword();
        LOG.warnf(
                "[SEED] Password not configured for '%s'. Generated password "
                        + "(note it down and change it): %s",
                username,
                generated);
        return generated;
    }

    private static String generateRandomPassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
