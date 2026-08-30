package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.JwtKeyPairTestResource;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InvalidEstimateDecisionTokenException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateDecisionToken;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateDecision;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("JwtEstimateDecisionTokenAdapter — Integration Tests")
class JwtEstimateDecisionTokenAdapterIT {

    @Inject
    JwtEstimateDecisionTokenAdapter adapter;

    @Nested
    @DisplayName("assinatura e conferência")
    class SignAndRead {

        @Test
        @DisplayName("devolve o identificador do token que assinou")
        void readsBackTheSignedTokenId() {
            EstimateDecisionToken token = decisionToken();

            assertEquals(token.getId(), adapter.readTokenId(adapter.sign(token)));
        }

        @Test
        @DisplayName("assina cada token com um identificador próprio")
        void signsEachTokenDistinctly() {
            assertNotEquals(adapter.readTokenId(adapter.sign(decisionToken())),
                    adapter.readTokenId(adapter.sign(decisionToken())));
        }

        @Test
        @DisplayName("mantém o link válido além da janela de decisão, para o registro poder dizer que expirou")
        void keepsSignatureValidBeyondTheDecisionWindow() {
            EstimateDecisionToken expired = decisionToken();
            expired.setExpiresAt(LocalDateTime.now().minusDays(1));

            assertEquals(expired.getId(), adapter.readTokenId(adapter.sign(expired)));
        }
    }

    @Nested
    @DisplayName("links recusados")
    class RejectedLinks {

        @Test
        @DisplayName("recusa um texto que não é um token")
        void rejectsGarbage() {
            assertThrows(InvalidEstimateDecisionTokenException.class,
                    () -> adapter.readTokenId("nao-e-um-token"));
        }

        @Test
        @DisplayName("recusa um token adulterado depois de assinado")
        void rejectsTamperedToken() {
            String signed = adapter.sign(decisionToken());
            String tampered = signed.substring(0, signed.lastIndexOf('.') + 1) + "assinaturaFalsa";

            assertThrows(InvalidEstimateDecisionTokenException.class,
                    () -> adapter.readTokenId(tampered));
        }

        @Test
        @DisplayName("recusa um token de login, que não foi emitido para decidir orçamento")
        void rejectsTokenIssuedForAnotherPurpose() {
            String loginToken = Jwt.issuer("oficina-api")
                    .subject(UUID.randomUUID().toString())
                    .groups(Set.of("ADMIN"))
                    .expiresIn(Duration.ofHours(1))
                    .sign();

            assertThrows(InvalidEstimateDecisionTokenException.class,
                    () -> adapter.readTokenId(loginToken));
        }
    }

    private EstimateDecisionToken decisionToken() {
        return EstimateDecisionToken.issue(UUID.randomUUID(), UUID.randomUUID(),
                EstimateDecision.APPROVE, LocalDateTime.now());
    }
}
