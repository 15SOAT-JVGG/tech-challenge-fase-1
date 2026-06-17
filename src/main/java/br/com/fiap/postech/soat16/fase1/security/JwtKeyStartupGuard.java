package br.com.fiap.postech.soat16.fase1.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;

/**
 * Fails fast at boot if the application starts in production (LaunchMode.NORMAL) still using
 * the default development JWT key. In dev/test it only logs a warning.
 * Production must override JWT_PRIVATE_KEY_LOCATION / JWT_PUBLIC_KEY_LOCATION pointing
 * to secure keys (e.g. a mounted secret), never the keys versioned in the repository.
 */
@ApplicationScoped
public class JwtKeyStartupGuard {

    private static final Logger LOG = Logger.getLogger(JwtKeyStartupGuard.class);

    // Same default defined in application.yaml for smallrye.jwt.sign.key.location
    static final String DEV_KEY_LOCATION = "jwt/privateKey.pem";

    @ConfigProperty(name = "smallrye.jwt.sign.key.location")
    String signKeyLocation;

    void onStart(@Observes StartupEvent event) {
        if (!DEV_KEY_LOCATION.equals(signKeyLocation)) {
            return;
        }

        if (LaunchMode.current() == LaunchMode.NORMAL) {
            throw new IllegalStateException(
                "Development JWT key (" + DEV_KEY_LOCATION + ") detected in production. "
                + "Set JWT_PRIVATE_KEY_LOCATION and JWT_PUBLIC_KEY_LOCATION pointing to secure keys.");
        }

        LOG.warnf("Using development JWT key (%s). DO NOT use in production.", DEV_KEY_LOCATION);
    }
}
