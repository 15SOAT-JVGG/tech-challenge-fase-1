package br.com.fiap.postech.soat16.fase1.auth.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AppUser entity — Unit Tests")
class AppUserTest {

    private static final String USERNAME = "admin";
    private static final String HASHED_PASSWORD = "$2a$10$hashedvalue";
    private static final String ROLE = "ADMIN";

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("public constructor sets fields and starts active")
        void publicConstructorSetsFieldsAndIsActive() {
            AppUser user = new AppUser(USERNAME, HASHED_PASSWORD, ROLE);

            assertEquals(USERNAME, user.getUsername());
            assertEquals(HASHED_PASSWORD, user.getPassword());
            assertEquals(ROLE, user.getRole());
            assertTrue(user.isActive());
            assertEquals(Boolean.TRUE, user.getActive());
        }

        @Test
        @DisplayName("protected no-arg constructor keeps the active default")
        void noArgConstructorLeavesFieldsUnset() {
            AppUser user = new AppUser();

            assertNull(user.getUsername());
            assertNull(user.getPassword());
            assertNull(user.getRole());
            assertTrue(user.isActive());
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("prePersist sets createdAt")
        void prePersistSetsCreatedAt() {
            AppUser user = new AppUser(USERNAME, HASHED_PASSWORD, ROLE);

            user.prePersist();

            assertNotNull(user.getCreatedAt());
        }

        @Test
        @DisplayName("deactivate flips active to false")
        void deactivateFlipsActiveToFalse() {
            AppUser user = new AppUser(USERNAME, HASHED_PASSWORD, ROLE);

            user.deactivate();

            assertFalse(user.isActive());
            assertEquals(Boolean.FALSE, user.getActive());
        }

        @Test
        @DisplayName("activate flips active back to true")
        void activateFlipsActiveBackToTrue() {
            AppUser user = new AppUser(USERNAME, HASHED_PASSWORD, ROLE);
            user.deactivate();

            user.activate();

            assertTrue(user.isActive());
        }

        @Test
        @DisplayName("changePassword replaces the stored hash")
        void changePasswordReplacesHash() {
            AppUser user = new AppUser(USERNAME, HASHED_PASSWORD, ROLE);

            user.changePassword("$2a$10$newhash");

            assertEquals("$2a$10$newhash", user.getPassword());
        }
    }
}
