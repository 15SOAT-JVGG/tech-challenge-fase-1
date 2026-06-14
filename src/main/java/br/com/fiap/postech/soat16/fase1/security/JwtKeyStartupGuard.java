package br.com.fiap.postech.soat16.fase1.security;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Falha rápido no boot se a aplicação subir em produção (LaunchMode.NORMAL) ainda usando
 * a chave JWT default de desenvolvimento. Em dev/test apenas registra um aviso.
 *
 * Produção deve sobrescrever JWT_PRIVATE_KEY_LOCATION / JWT_PUBLIC_KEY_LOCATION apontando
 * para chaves seguras (ex.: secret montado), nunca as chaves versionadas no repositório.
 */
@ApplicationScoped
public class JwtKeyStartupGuard {

    private static final Logger LOG = Logger.getLogger(JwtKeyStartupGuard.class);

    // Mesmo default definido em application.yaml para smallrye.jwt.sign.key.location
    static final String DEV_KEY_LOCATION = "jwt/privateKey.pem";

    @ConfigProperty(name = "smallrye.jwt.sign.key.location")
    String signKeyLocation;

    void onStart(@Observes StartupEvent event) {
        if (!DEV_KEY_LOCATION.equals(signKeyLocation)) {
            return;
        }

        if (LaunchMode.current() == LaunchMode.NORMAL) {
            throw new IllegalStateException(
                "Chave JWT de desenvolvimento (" + DEV_KEY_LOCATION + ") detectada em produção. "
                + "Defina JWT_PRIVATE_KEY_LOCATION e JWT_PUBLIC_KEY_LOCATION apontando para chaves seguras.");
        }

        LOG.warnf("Usando chave JWT de desenvolvimento (%s). NÃO utilizar em produção.", DEV_KEY_LOCATION);
    }
}
