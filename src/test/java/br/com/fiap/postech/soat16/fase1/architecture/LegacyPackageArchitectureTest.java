package br.com.fiap.postech.soat16.fase1.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Organização dos pacotes compartilhados")
class LegacyPackageArchitectureTest {

    private static final Path SOURCE_ROOT = Path.of(
            System.getProperty("user.dir"),
            "src/main/java/br/com/fiap/postech/soat16/fase1");
    private static final List<String> LEGACY_PACKAGES = List.of(
            "controller",
            "dto",
            "exception",
            "mapper",
            "model",
            "repository",
            "security",
            "service");

    @Test
    @DisplayName("não restam classes nos pacotes horizontais legados")
    void noLegacyHorizontalSlicesRemain() throws IOException {
        List<Path> violations;
        try (var files = Files.walk(SOURCE_ROOT)) {
            violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::isInsideLegacyPackage)
                    .toList();
        }

        assertTrue(violations.isEmpty(), () -> "Pacotes legados: " + violations);
    }

    private boolean isInsideLegacyPackage(Path source) {
        Path relative = SOURCE_ROOT.relativize(source);
        return relative.getNameCount() > 1
                && LEGACY_PACKAGES.contains(relative.getName(0).toString());
    }
}
