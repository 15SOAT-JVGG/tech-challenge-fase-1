package br.com.fiap.postech.soat16.customerauth.adapter.out.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Lê a chave no formato que {@code infra/scripts/generate-jwt-pair.sh} gera: PKCS#8 em PEM, o mesmo
 * arquivo montado no pod da API.
 */
public final class PemPrivateKeys {

    private static final String PKCS8_LABEL = "PRIVATE KEY";
    private static final String PKCS8_HEADER = "-----BEGIN %s-----".formatted(PKCS8_LABEL);
    private static final String PKCS8_FOOTER = "-----END %s-----".formatted(PKCS8_LABEL);

    private PemPrivateKeys() {
    }

    public static RSAPrivateKey parse(String pem) {
        if (pem == null || !pem.contains(PKCS8_HEADER)) {
            throw new IllegalArgumentException("A chave privada precisa estar em PEM PKCS#8 (" + PKCS8_LABEL + ")");
        }
        String body = pem.replace(PKCS8_HEADER, "").replace(PKCS8_FOOTER, "").replaceAll("\\s", "");
        try {
            byte[] encoded = Base64.getDecoder().decode(body);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new IllegalArgumentException("Chave privada RSA ilegível", exception);
        }
    }
}
