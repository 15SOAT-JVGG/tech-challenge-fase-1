package br.com.fiap.postech.soat16.fase1.shared.test.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Gera um par RS256 efêmero para os testes de integração e aponta a configuração da aplicação
 * para ele. Nenhuma chave é versionada ou embutida na imagem: cada execução assina e valida
 * com um par recém-criado dentro de target/, do mesmo modo que os ambientes reais recebem o
 * par por caminho externo.
 */
public class JwtKeyPairTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Path KEY_DIRECTORY = Path.of("target", "jwt-test");
    private static final int KEY_SIZE_BITS = 2048;
    private static final int PEM_LINE_LENGTH = 64;

    @Override
    public Map<String, String> start() {
        KeyPair keyPair = generateKeyPair();
        Path privateKeyLocation = writePem(
                "privateKey.pem", "PRIVATE KEY", keyPair.getPrivate().getEncoded());
        Path publicKeyLocation = writePem(
                "publicKey.pem", "PUBLIC KEY", keyPair.getPublic().getEncoded());

        return Map.of(
                "smallrye.jwt.sign.key.location", privateKeyLocation.toString(),
                "mp.jwt.verify.publickey.location", publicKeyLocation.toString());
    }

    @Override
    public void stop() {
        // O par vive em target/ e desaparece com o `mvn clean`.
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE_BITS);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("RSA indisponível na JVM de teste", exception);
        }
    }

    private Path writePem(String fileName, String label, byte[] encodedKey) {
        String base64 = Base64.getMimeEncoder(PEM_LINE_LENGTH, new byte[] {'\n'})
                .encodeToString(encodedKey);
        String pem = "-----BEGIN %s-----\n%s\n-----END %s-----\n".formatted(label, base64, label);

        try {
            Files.createDirectories(KEY_DIRECTORY);
            Path location = KEY_DIRECTORY.resolve(fileName);
            Files.writeString(location, pem, StandardCharsets.US_ASCII);
            return location;
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha ao gravar o par RS256 de teste", exception);
        }
    }
}
