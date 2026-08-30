package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.JwtKeyPairTestResource;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.ExpiredWorkOrderTrackingTokenException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InvalidWorkOrderTrackingTokenException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderTrackingToken;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("JwtWorkOrderTrackingTokenAdapter — Integration Tests")
class JwtWorkOrderTrackingTokenAdapterIT {

    @Inject
    JwtWorkOrderTrackingTokenAdapter adapter;

    @Nested
    @DisplayName("assinatura e conferência")
    class SignAndRead {

        @Test
        @DisplayName("devolve a ordem de serviço e o prazo do link que assinou")
        void readsBackTheSignedTracking() {
            WorkOrderTrackingToken token = trackingToken();

            WorkOrderTrackingToken read = adapter.read(adapter.sign(token));

            assertEquals(token.workOrderId(), read.workOrderId());
            assertEquals(token.issuedAt(), read.issuedAt());
            assertEquals(token.expiresAt(), read.expiresAt());
        }

        @Test
        @DisplayName("assina cada acompanhamento com a sua própria ordem de serviço")
        void signsEachWorkOrderDistinctly() {
            WorkOrderTrackingToken first = trackingToken();
            WorkOrderTrackingToken second = trackingToken();

            assertEquals(first.workOrderId(), adapter.read(adapter.sign(first)).workOrderId());
            assertEquals(second.workOrderId(), adapter.read(adapter.sign(second)).workOrderId());
        }

        @Test
        @DisplayName("mantém o link legível além dos trinta dias, para o domínio poder dizer que expirou")
        void keepsSignatureValidBeyondTheTrackingWindow() {
            WorkOrderTrackingToken expired = WorkOrderTrackingToken.issue(
                    UUID.randomUUID(), LocalDateTime.now().minusDays(45).truncatedTo(ChronoUnit.SECONDS));

            WorkOrderTrackingToken read = adapter.read(adapter.sign(expired));

            assertEquals(expired.expiresAt(), read.expiresAt());
            assertThrows(ExpiredWorkOrderTrackingTokenException.class,
                    () -> read.ensureValidAt(LocalDateTime.now()));
        }
    }

    @Nested
    @DisplayName("links recusados")
    class RejectedLinks {

        @Test
        @DisplayName("recusa um texto que não é um token")
        void rejectsGarbage() {
            assertThrows(InvalidWorkOrderTrackingTokenException.class,
                    () -> adapter.read("nao-e-um-token"));
        }

        @Test
        @DisplayName("recusa um token adulterado depois de assinado")
        void rejectsTamperedToken() {
            String signed = adapter.sign(trackingToken());
            String tampered = signed.substring(0, signed.lastIndexOf('.') + 1) + "assinaturaFalsa";

            assertThrows(InvalidWorkOrderTrackingTokenException.class,
                    () -> adapter.read(tampered));
        }

        @Test
        @DisplayName("recusa um token de acompanhamento que não diz qual é a ordem de serviço")
        void rejectsTokenWithoutWorkOrder() {
            String withoutWorkOrder = Jwt.issuer("oficina-api")
                    .claim("purpose", "work-order-tracking")
                    .expiresIn(Duration.ofHours(1))
                    .sign();

            assertThrows(InvalidWorkOrderTrackingTokenException.class,
                    () -> adapter.read(withoutWorkOrder));
        }

        @Test
        @DisplayName("recusa um token de login, que não foi emitido para acompanhar uma ordem")
        void rejectsTokenIssuedForAnotherPurpose() {
            String loginToken = Jwt.issuer("oficina-api")
                    .subject(UUID.randomUUID().toString())
                    .groups(Set.of("ADMIN"))
                    .expiresIn(Duration.ofHours(1))
                    .sign();

            assertThrows(InvalidWorkOrderTrackingTokenException.class,
                    () -> adapter.read(loginToken));
        }
    }

    private WorkOrderTrackingToken trackingToken() {
        return WorkOrderTrackingToken.issue(UUID.randomUUID(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }
}
