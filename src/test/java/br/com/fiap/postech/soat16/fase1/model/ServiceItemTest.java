package br.com.fiap.postech.soat16.fase1.model;

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
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
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
        @DisplayName("a transient entity (id == null) is never equal to another transient entity")
        void transientEntitiesAreNeverEqual() {
            ServiceItem a = serviceItem();
            ServiceItem b = serviceItem();

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("a transient entity is equal to itself")
        void transientEntityEqualsItself() {
            ServiceItem a = serviceItem();

            assertEquals(a, a);
        }

        @Test
        @DisplayName("two persisted entities with the same id are equal")
        void equalWhenIdsMatch() {
            UUID id = UUID.randomUUID();
            ServiceItem a = serviceItem();
            ServiceItem b = serviceItem();
            setId(a, id);
            setId(b, id);
            b.setActive(false);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("two persisted entities with different ids are not equal")
        void notEqualWhenIdsDiffer() {
            ServiceItem a = serviceItem();
            ServiceItem b = serviceItem();
            setId(a, UUID.randomUUID());
            setId(b, UUID.randomUUID());

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("not equal to null or to an unrelated type")
        void notEqualToNullOrUnrelatedType() {
            ServiceItem a = serviceItem();
            setId(a, UUID.randomUUID());

            assertNotEquals(null, a);
            assertNotEquals("not-a-service-item", a);
        }

        @Test
        @DisplayName("hashCode is stable and based on the runtime class, not the id")
        void hashCodeIsClassBased() {
            ServiceItem a = serviceItem();
            ServiceItem b = serviceItem();
            setId(a, UUID.randomUUID());
            setId(b, UUID.randomUUID());

            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    @Nested
    @DisplayName("active flag")
    class ActiveFlag {

        @Test
        @DisplayName("can be deactivated")
        void canBeDeactivated() {
            ServiceItem serviceItem = serviceItem();

            serviceItem.setActive(false);

            assertFalse(serviceItem.isActive());
        }
    }
}
