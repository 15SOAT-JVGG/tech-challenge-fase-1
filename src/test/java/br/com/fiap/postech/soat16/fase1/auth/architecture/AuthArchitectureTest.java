package br.com.fiap.postech.soat16.fase1.auth.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dependências da arquitetura de autenticação")
class AuthArchitectureTest {

    private static final Path SOURCE_ROOT = Path.of(
            System.getProperty("user.dir"),
            "src/main/java/br/com/fiap/postech/soat16/fase1");
    private static final Path AUTH_ROOT = SOURCE_ROOT.resolve("auth");

    @Test
    @DisplayName("a aplicação depende apenas de portas e domínio")
    void applicationDependsOnlyOnPortsAndDomain() throws IOException {
        assertNoForbiddenImports(
                AUTH_ROOT.resolve("application"),
                List.of(
                        ".auth.adapter.",
                        ".controller.",
                        ".dto.",
                        ".repository.",
                        ".security.",
                        "org.eclipse.microprofile.config."));
    }

    @Test
    @DisplayName("o domínio não depende da aplicação nem de adaptadores")
    void domainRemainsIndependentFromOuterLayers() throws IOException {
        assertNoForbiddenImports(
                AUTH_ROOT.resolve("domain"),
                List.of(
                        ".auth.application.",
                        ".auth.adapter.",
                        ".controller.",
                        ".dto.",
                        ".repository.",
                        ".security."));
    }

    @Test
    @DisplayName("a persistência implementa a porta da aplicação")
    void persistenceImplementsApplicationPort() throws IOException {
        String source = Files.readString(AUTH_ROOT.resolve(
                "adapter/out/persistence/AppUserRepository.java"));

        assertTrue(source.contains("implements PanacheRepository<AppUserJpaEntity>, "
                + "AppUserPersistencePort"));
        assertFalse(source.contains("auth.application.AuthService"));
    }

    @Test
    @DisplayName("o bootstrap chama a aplicação")
    void startupAdapterCallsApplication() throws IOException {
        String source = Files.readString(AUTH_ROOT.resolve(
                "adapter/in/startup/DataSeeder.java"));

        assertTrue(source.contains("auth.application.AuthService"));
        assertFalse(source.contains("adapter.out.persistence.AppUserRepository"));
        assertFalse(source.contains("shared.infrastructure.security.PasswordService"));
    }

    private void assertNoForbiddenImports(
            Path sourceRoot,
            List<String> forbiddenPackages
    ) throws IOException {
        List<String> violations;
        try (var files = Files.walk(sourceRoot)) {
            violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> importsFrom(path).stream())
                    .filter(line -> forbiddenPackages.stream().anyMatch(line::contains))
                    .toList();
        }
        assertTrue(violations.isEmpty(), () -> "Dependências proibidas: " + violations);
    }

    private List<String> importsFrom(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .map(line -> AUTH_ROOT.relativize(path) + ": " + line)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao ler " + path, exception);
        }
    }
}
