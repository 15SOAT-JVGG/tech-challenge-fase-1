package br.com.fiap.postech.soat16.customerauth.support;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Par RS256 efêmero, gerado por execução, como o {@code JwtKeyPairTestResource} da API.
 */
public final class TestKeys {

    private static final int KEY_SIZE_BITS = 2048;
    private static final KeyPair KEY_PAIR = generate();

    private TestKeys() {
    }

    public static KeyPair keyPair() {
        return KEY_PAIR;
    }

    public static String privateKeyPem() {
        String body = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(KEY_PAIR.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";
    }

    public static String privateKeyPemBase64() {
        return Base64.getEncoder().encodeToString(privateKeyPem().getBytes(StandardCharsets.US_ASCII));
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE_BITS);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
