package br.com.fiap.postech.soat16.fase1.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.service.PasswordService;

import io.quarkus.runtime.StartupEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataSeeder — Unit Tests")
class DataSeederTest {

    @Mock
    private AppUserRepository repository;

    @Mock
    private PasswordService passwordService;

    @Test
    @DisplayName("does not touch the repository when seeding is disabled")
    void doesNothingWhenSeedDisabled() {
        DataSeeder seeder = new DataSeeder(repository, passwordService);
        seeder.seedEnabled = false;

        assertDoesNotThrow(() -> seeder.onStart(new StartupEvent()));

        verifyNoInteractions(repository);
    }
}
