package br.com.fiap.postech.soat16.fase1.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.Part;
import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("PartRepository — Integration Tests")
class PartRepositoryIT {

    @Inject
    PartRepository repository;

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable e) {
            throw new IllegalStateException("Falha ao preparar dados de teste", e);
        }
    }

    private Part seed(String name, int stockQuantity, int minimumStock) {
        Part part = new Part(name, "desc", new BigDecimal("10.00"), stockQuantity, "UN", minimumStock, null);
        return inTransaction(() -> repository.persist(part));
    }

    @Test
    @DisplayName("findLowStock includes parts at or below their minimum stock")
    void findLowStockIncludesPartsAtOrBelowMinimum() {
        Part low = seed("Item em baixa - " + java.util.UUID.randomUUID(), 5, 5);

        List<Part> lowStock = inTransaction(() -> repository.findLowStock());

        assertTrue(lowStock.stream().anyMatch(p -> p.getId().equals(low.getId())));
    }

    @Test
    @DisplayName("findLowStock excludes parts above their minimum stock")
    void findLowStockExcludesPartsAboveMinimum() {
        Part healthy = seed("Item saudavel - " + java.util.UUID.randomUUID(), 100, 5);

        List<Part> lowStock = inTransaction(() -> repository.findLowStock());

        assertFalse(lowStock.stream().anyMatch(p -> p.getId().equals(healthy.getId())));
    }
}
