package br.com.fiap.postech.soat16.customerauth.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.customerauth.application.IssuedToken;
import br.com.fiap.postech.soat16.customerauth.domain.Customer;
import br.com.fiap.postech.soat16.customerauth.support.TestKeys;

@DisplayName("RsaJwtCustomerTokenAdapter — Testes unitários")
class RsaJwtCustomerTokenAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("contrato MicroProfile JWT esperado pela API")
    class Contract {

        private final RsaJwtCustomerTokenAdapter adapter = new RsaJwtCustomerTokenAdapter(
                PemPrivateKeys.parse(TestKeys.privateKeyPem()),
                "oficina-api",
                Duration.ofMinutes(60),
                Clock.fixed(NOW, ZoneOffset.UTC));

        @Test
        @DisplayName("deve assinar em RS256 com a chave privada do par da API")
        void shouldSignWithRs256() throws Exception {
            SignedJWT jwt = SignedJWT.parse(adapter.issueFor(new Customer(CUSTOMER_ID, true)).token());

            assertEquals(JWSAlgorithm.RS256, jwt.getHeader().getAlgorithm());
            assertTrue(jwt.verify(new RSASSAVerifier((RSAPublicKey) TestKeys.keyPair().getPublic())));
        }

        @Test
        @DisplayName("deve levar issuer, sub com o id do cliente, grupo CUSTOMER e validade")
        void shouldCarryExpectedClaims() throws Exception {
            IssuedToken issued = adapter.issueFor(new Customer(CUSTOMER_ID, true));
            JWTClaimsSet claims = SignedJWT.parse(issued.token()).getJWTClaimsSet();

            assertEquals("oficina-api", claims.getIssuer());
            assertEquals(CUSTOMER_ID.toString(), claims.getSubject());
            assertEquals(List.of("CUSTOMER"), claims.getStringListClaim("groups"));
            assertEquals(NOW, claims.getIssueTime().toInstant());
            assertEquals(NOW.plus(Duration.ofMinutes(60)), claims.getExpirationTime().toInstant());
            assertNotNull(claims.getJWTID());
            assertEquals(CUSTOMER_ID, issued.customerId());
            assertEquals("CUSTOMER", issued.role());
            assertEquals(3600, issued.expiresIn());
        }
    }

    @Nested
    @DisplayName("leitura da chave privada")
    class PrivateKey {

        private static String pem(String label, String body) {
            return "-----BEGIN %1$s-----%n%2$s%n-----END %1$s-----".formatted(label, body);
        }

        @Test
        @DisplayName("deve recusar chave fora do formato PEM PKCS#8")
        void shouldRejectNonPkcs8Pem() {
            assertThrows(IllegalArgumentException.class, () -> PemPrivateKeys.parse(pem("RSA PRIVATE KEY", "AAAA")));
            assertThrows(IllegalArgumentException.class, () -> PemPrivateKeys.parse(null));
        }

        @Test
        @DisplayName("deve recusar PEM com conteúdo corrompido")
        void shouldRejectCorruptedPem() {
            assertThrows(IllegalArgumentException.class, () -> PemPrivateKeys.parse(pem("PRIVATE KEY", "AAAA")));
        }
    }
}
