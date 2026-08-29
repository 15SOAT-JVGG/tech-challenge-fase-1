package br.com.fiap.postech.soat16.fase1.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Empacotamento da chave de assinatura JWT")
class JwtKeyPackagingTest {

    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path PACKAGED_RESOURCES = PROJECT_ROOT.resolve("src/main/resources");
    private static final Path DOCKERFILE = PROJECT_ROOT.resolve("infra/docker/Dockerfile");

    private static final String PRIVATE_KEY_MARKER = "PRIVATE KEY-----";

    @Test
    @DisplayName("nenhuma chave privada vai para o classpath da aplicação")
    void noPrivateKeyIsPackagedIntoTheArtifact() throws IOException {
        List<Path> offendingResources;
        try (var resources = Files.walk(PACKAGED_RESOURCES)) {
            offendingResources = resources
                    .filter(Files::isRegularFile)
                    .filter(JwtKeyPackagingTest::containsPrivateKey)
                    .map(PROJECT_ROOT::relativize)
                    .toList();
        }

        assertTrue(offendingResources.isEmpty(),
                () -> "Chave privada empacotada na imagem: " + offendingResources);
    }

    @Test
    @DisplayName("o build da imagem não gera nem copia material de chave")
    void imageBuildDoesNotProduceKeyMaterial() throws IOException {
        String dockerfile = Files.readString(DOCKERFILE);

        assertFalse(dockerfile.contains("genpkey"),
                "O build não deve gerar chaves: elas vêm de configuração externa");
        assertFalse(dockerfile.contains(".pem"),
                "O build não deve copiar chaves: elas são montadas em tempo de execução");
    }

    private static boolean containsPrivateKey(Path resource) {
        try {
            // ISO_8859_1 mapeia qualquer byte para um caractere, então a busca também
            // funciona em recursos binários sem risco de erro de decodificação.
            return new String(Files.readAllBytes(resource), StandardCharsets.ISO_8859_1)
                    .contains(PRIVATE_KEY_MARKER);
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao ler " + resource, exception);
        }
    }
}
