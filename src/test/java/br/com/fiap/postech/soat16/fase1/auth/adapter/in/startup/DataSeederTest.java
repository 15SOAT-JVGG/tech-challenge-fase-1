package br.com.fiap.postech.soat16.fase1.auth.adapter.in.startup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.auth.application.AuthService;

import io.quarkus.runtime.StartupEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataSeeder — Unit Tests")
class DataSeederTest {

    @Mock
    private AuthService authService;

    @Test
    @DisplayName("does not call authentication use cases when seeding is disabled")
    void doesNothingWhenSeedDisabled() {
        DataSeeder seeder = new DataSeeder(authService);
        seeder.seedEnabled = false;

        assertDoesNotThrow(() -> seeder.onStart(new StartupEvent()));

        verifyNoInteractions(authService);
    }
}
