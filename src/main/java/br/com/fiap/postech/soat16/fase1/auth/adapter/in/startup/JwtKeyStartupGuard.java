package br.com.fiap.postech.soat16.fase1.auth.adapter.in.startup;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;

/**
 * Impede a inicialização em produção com a chave JWT de desenvolvimento. Os ambientes produtivos
 * devem fornecer chaves próprias por JWT_PRIVATE_KEY_LOCATION e JWT_PUBLIC_KEY_LOCATION.
 */
@ApplicationScoped
public class JwtKeyStartupGuard {

    private static final Logger LOG = Logger.getLogger(JwtKeyStartupGuard.class);

    static final String DEV_KEY_LOCATION = ".local-jwt/privateKey.pem";

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
