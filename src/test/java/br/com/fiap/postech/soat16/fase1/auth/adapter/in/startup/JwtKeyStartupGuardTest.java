package br.com.fiap.postech.soat16.fase1.auth.adapter.in.startup;

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
        @DisplayName("fails fast in production")
        void failsFastInProduction() {
            LaunchMode.set(LaunchMode.NORMAL);
            JwtKeyStartupGuard guard =
                    guardWithKeyLocation(JwtKeyStartupGuard.DEV_KEY_LOCATION);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> guard.onStart(new StartupEvent()));

            assertTrue(exception.getMessage().contains(
                    JwtKeyStartupGuard.DEV_KEY_LOCATION));
        }

        @Test
        @DisplayName("only warns in development mode")
        void onlyWarnsInDevelopment() {
            LaunchMode.set(LaunchMode.DEVELOPMENT);

            assertDoesNotThrow(() -> guardWithKeyLocation(
                    JwtKeyStartupGuard.DEV_KEY_LOCATION)
                    .onStart(new StartupEvent()));
        }

        @Test
        @DisplayName("only warns in test mode")
        void onlyWarnsInTest() {
            LaunchMode.set(LaunchMode.TEST);

            assertDoesNotThrow(() -> guardWithKeyLocation(
                    JwtKeyStartupGuard.DEV_KEY_LOCATION)
                    .onStart(new StartupEvent()));
        }
    }

    @Nested
    @DisplayName("custom key configured")
    class CustomKeyConfigured {

        @Test
        @DisplayName("does nothing in production with a custom key")
        void noOpWhenKeyLocationIsOverridden() {
            LaunchMode.set(LaunchMode.NORMAL);

            assertDoesNotThrow(() -> guardWithKeyLocation(
                    "file:/etc/jwt/privateKey.pem")
                    .onStart(new StartupEvent()));
        }
    }
}
