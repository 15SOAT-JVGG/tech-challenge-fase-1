package br.com.fiap.postech.soat16.fase1.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;

@DisplayName("JwtKeyStartupGuard — Unit Tests")
class JwtKeyStartupGuardTest {

    @AfterEach
    void resetLaunchMode() {
        // Avoids leaking launch mode state into other test classes sharing this JVM/fork.
        LaunchMode.set(LaunchMode.TEST);
    }

    private JwtKeyStartupGuard guardWithKeyLocation(String location) {
        JwtKeyStartupGuard guard = new JwtKeyStartupGuard();
        guard.signKeyLocation = location;
        return guard;
    }

    @Nested
    @DisplayName("development key detected")
    class DevelopmentKeyDetected {

        @Test
        @DisplayName("fails fast in production (LaunchMode.NORMAL)")
        void failsFastInProduction() {
            LaunchMode.set(LaunchMode.NORMAL);
            JwtKeyStartupGuard guard = guardWithKeyLocation(JwtKeyStartupGuard.DEV_KEY_LOCATION);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guard.onStart(new StartupEvent()));

            assertTrue(ex.getMessage().contains(JwtKeyStartupGuard.DEV_KEY_LOCATION));
        }

        @Test
        @DisplayName("only warns (does not throw) in development mode")
        void onlyWarnsInDevelopment() {
            LaunchMode.set(LaunchMode.DEVELOPMENT);
            JwtKeyStartupGuard guard = guardWithKeyLocation(JwtKeyStartupGuard.DEV_KEY_LOCATION);

            assertDoesNotThrow(() -> guard.onStart(new StartupEvent()));
        }

        @Test
        @DisplayName("only warns (does not throw) in test mode")
        void onlyWarnsInTest() {
            LaunchMode.set(LaunchMode.TEST);
            JwtKeyStartupGuard guard = guardWithKeyLocation(JwtKeyStartupGuard.DEV_KEY_LOCATION);

            assertDoesNotThrow(() -> guard.onStart(new StartupEvent()));
        }
    }

    @Nested
    @DisplayName("custom key configured")
    class CustomKeyConfigured {

        @Test
        @DisplayName("does nothing, even in production, when a non-default key location is set")
        void noOpWhenKeyLocationIsOverridden() {
            LaunchMode.set(LaunchMode.NORMAL);
            JwtKeyStartupGuard guard = guardWithKeyLocation("file:/etc/jwt/privateKey.pem");

            assertDoesNotThrow(() -> guard.onStart(new StartupEvent()));
        }
    }
}
