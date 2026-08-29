package br.com.fiap.postech.soat16.fase1.customer.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dependências da arquitetura de clientes")
class CustomerArchitectureTest {

    private static final Path CUSTOMER_ROOT = Path.of(
            System.getProperty("user.dir"),
            "src/main/java/br/com/fiap/postech/soat16/fase1/customer");

    @Test
    @DisplayName("a aplicação não depende de adaptadores nem de transporte")
    void applicationDependsOnlyOnPortsAndDomain() throws IOException {
        assertNoForbiddenImports(
                CUSTOMER_ROOT.resolve("application"),
                List.of(
                        ".customer.adapter.",
                        ".vehicle.adapter.",
                        ".repository.",
                        ".controller.",
                        ".dto."));
    }

    @Test
    @DisplayName("o domínio não depende da aplicação nem de adaptadores")
    void domainRemainsIndependentFromOuterLayers() throws IOException {
        assertNoForbiddenImports(
                CUSTOMER_ROOT.resolve("domain"),
                List.of(
                        ".customer.application.",
                        ".customer.adapter.",
                        ".repository.",
                        ".controller.",
                        ".dto.",
                        ".service."));
    }

    private void assertNoForbiddenImports(Path sourceRoot, List<String> forbiddenPackages)
            throws IOException {
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
                    .map(line -> CUSTOMER_ROOT.relativize(path) + ": " + line)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao ler " + path, exception);
        }
    }
}
