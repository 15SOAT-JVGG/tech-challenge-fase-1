package br.com.fiap.postech.soat16.fase1.servicecatalog.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.servicecatalog.application.command.CreateServiceItemCommand;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.command.DeleteServiceItemCommand;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.command.FindServiceItemQuery;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.command.UpdateServiceItemCommand;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.port.out.ServiceCatalogPersistencePort;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.result.ServiceItemResult;
import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ResourceNotFoundException;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceItemService — Unit Tests")
class ServiceItemServiceTest {

    private static final UUID ID = UUID.randomUUID();

    @Mock
    private ServiceCatalogPersistencePort serviceCatalogPersistence;

    private ServiceItemService service;
    private ServiceItem entity;

    @BeforeEach
    void setUp() {
        service = new ServiceItemService(serviceCatalogPersistence);
        entity = new ServiceItem();
        entity.setId(ID);
        entity.setName("Troca de óleo");
        entity.setDescription("Troca de óleo e filtro");
        entity.setBasePrice(new BigDecimal("120.00"));
        entity.setEstimatedDurationMinutes(30);
        entity.setActive(true);
    }

    private CreateServiceItemCommand createCommand(String name, Boolean active) {
        return new CreateServiceItemCommand(
                name, "desc", new BigDecimal("120.00"), 30, active);
    }

    private UpdateServiceItemCommand updateCommand(String name, Boolean active) {
        return new UpdateServiceItemCommand(
                ID, name, "desc", new BigDecimal("120.00"), 30, active);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("should map all services to result")
        void shouldMapAll() {
            when(serviceCatalogPersistence.listAllServiceItems())
                    .thenReturn(Uni.createFrom().item(List.of(entity)));

            List<ServiceItemResult> result = service.listAll().await().indefinitely();

            assertEquals(1, result.size());
            assertEquals("Troca de óleo", result.getFirst().name());
        }

        @Test
        @DisplayName("should return empty list when no services exist")
        void shouldReturnEmpty() {
            when(serviceCatalogPersistence.listAllServiceItems())
                    .thenReturn(Uni.createFrom().item(List.of()));

            assertTrue(service.listAll().await().indefinitely().isEmpty());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return service when found")
        void shouldReturnWhenFound() {
            when(serviceCatalogPersistence.findServiceItemById(ID))
                    .thenReturn(Uni.createFrom().item(entity));

            ServiceItemResult result =
                    service.findById(new FindServiceItemQuery(ID)).await().indefinitely();

            assertNotNull(result);
            assertEquals("Troca de óleo", result.name());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when missing")
        void shouldThrowWhenMissing() {
            when(serviceCatalogPersistence.findServiceItemById(ID))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.findById(new FindServiceItemQuery(ID))
                            .await().indefinitely());
        }
    }

    @Test
    @DisplayName("create should persist service and default active to true when omitted")
    void createShouldPersistDefaultingActive() {
        when(serviceCatalogPersistence.save(any(ServiceItem.class)))
                .thenAnswer(invocation -> {
                    ServiceItem saved = invocation.getArgument(0, ServiceItem.class);
                    return Uni.createFrom().item(saved);
                });

        ServiceItemResult result =
                service.create(createCommand("Alinhamento", null)).await().indefinitely();

        assertNotNull(result);
        assertTrue(result.active());
        verify(serviceCatalogPersistence).save(any(ServiceItem.class));
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update entity and return result")
        void shouldUpdate() {
            when(serviceCatalogPersistence.findServiceItemById(ID))
                    .thenReturn(Uni.createFrom().item(entity));
            when(serviceCatalogPersistence.save(entity))
                    .thenReturn(Uni.createFrom().item(entity));

            ServiceItemResult result =
                    service.update(updateCommand("Balanceamento", false)).await().indefinitely();

            assertNotNull(result);
            assertEquals("Balanceamento", entity.getName());
            assertFalse(entity.isActive());
            verify(serviceCatalogPersistence).save(entity);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when missing")
        void shouldThrowWhenMissing() {
            when(serviceCatalogPersistence.findServiceItemById(ID))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.update(updateCommand("X", true)).await().indefinitely());
            verify(serviceCatalogPersistence, never()).save(any(ServiceItem.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete when record exists")
        void shouldDelete() {
            when(serviceCatalogPersistence.deleteServiceItemById(ID))
                    .thenReturn(Uni.createFrom().item(true));

            assertDoesNotThrow(
                    () -> service.delete(new DeleteServiceItemCommand(ID))
                            .await().indefinitely());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when nothing was deleted")
        void shouldThrowWhenNothingDeleted() {
            when(serviceCatalogPersistence.deleteServiceItemById(ID))
                    .thenReturn(Uni.createFrom().item(false));

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.delete(new DeleteServiceItemCommand(ID))
                            .await().indefinitely());
        }
    }
}
