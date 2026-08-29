package br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ServiceItem model — Unit Tests")
class ServiceItemTest {

    private ServiceItem serviceItem() {
        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setName("Troca de oleo");
        serviceItem.setDescription("Troca de oleo e filtro");
        serviceItem.setBasePrice(new BigDecimal("120.00"));
        serviceItem.setEstimatedDurationMinutes(30);
        serviceItem.setActive(true);
        return serviceItem;
    }

    private void setId(ServiceItem serviceItem, UUID id) {
        try {
            Field field = ServiceItem.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(serviceItem, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    @DisplayName("allows setting and reading all basic fields")
    void shouldAllowSettingBasicFields() {
        ServiceItem serviceItem = serviceItem();

        assertEquals("Troca de oleo", serviceItem.getName());
        assertEquals("Troca de oleo e filtro", serviceItem.getDescription());
        assertEquals(new BigDecimal("120.00"), serviceItem.getBasePrice());
        assertEquals(30, serviceItem.getEstimatedDurationMinutes());
        assertTrue(serviceItem.isActive());
    }

    @Nested
    @DisplayName("equality (Hibernate proxy-safe pattern)")
    class Equality {

        @Test
        @DisplayName("a transient entity is never equal to another transient entity")
        void transientEntitiesAreNeverEqual() {
            ServiceItem first = serviceItem();
            ServiceItem second = serviceItem();

            assertNotEquals(first, second);
        }

        @Test
        @DisplayName("a transient entity is equal to itself")
        void transientEntityEqualsItself() {
            ServiceItem serviceItem = serviceItem();

            assertEquals(serviceItem, serviceItem);
        }

        @Test
        @DisplayName("two persisted entities with the same id are equal")
        void equalWhenIdsMatch() {
            UUID id = UUID.randomUUID();
            ServiceItem first = serviceItem();
            ServiceItem second = serviceItem();
            setId(first, id);
            setId(second, id);
            second.setActive(false);

            assertEquals(first, second);
            assertEquals(first.hashCode(), second.hashCode());
        }

        @Test
        @DisplayName("two persisted entities with different ids are not equal")
        void notEqualWhenIdsDiffer() {
            ServiceItem first = serviceItem();
            ServiceItem second = serviceItem();
            setId(first, UUID.randomUUID());
            setId(second, UUID.randomUUID());

            assertNotEquals(first, second);
        }

        @Test
        @DisplayName("not equal to null or to an unrelated type")
        void notEqualToNullOrUnrelatedType() {
            ServiceItem serviceItem = serviceItem();
            setId(serviceItem, UUID.randomUUID());

            assertNotEquals(null, serviceItem);
            assertNotEquals("not-a-service-item", serviceItem);
        }

        @Test
        @DisplayName("hashCode is stable and based on the runtime class")
        void hashCodeIsClassBased() {
            ServiceItem first = serviceItem();
            ServiceItem second = serviceItem();
            setId(first, UUID.randomUUID());
            setId(second, UUID.randomUUID());

            assertEquals(first.hashCode(), second.hashCode());
        }
    }

    @Test
    @DisplayName("active flag can be deactivated")
    void canBeDeactivated() {
        ServiceItem serviceItem = serviceItem();

        serviceItem.setActive(false);

        assertFalse(serviceItem.isActive());
    }
}
