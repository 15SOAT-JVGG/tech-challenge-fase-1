package br.com.fiap.postech.soat16.fase1.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotAuthorizedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.dto.request.LoginRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.LoginResponseDto;
import br.com.fiap.postech.soat16.fase1.service.PasswordService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Unit Tests")
class AuthServiceTest {

    private static final String USERNAME = "admin";
    private static final String RAW_PASSWORD = "S3nh@-F0rte";
    private static final String ROLE = "ADMIN";

    @Mock
    private AppUserRepository repository;

    private AuthService authService;
    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
        authService = new AuthService(repository, passwordService);
        authService.issuer = "oficina-api-test";
        authService.expirationHours = 8;
    }

    private AppUser activeUser() {
        return new AppUser(USERNAME, passwordService.hash(RAW_PASSWORD), ROLE);
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should issue a JWT with the user's role when credentials are valid")
        void shouldLoginWithValidCredentials() {
            when(repository.findByUsername(USERNAME)).thenReturn(Uni.createFrom().item(activeUser()));

            LoginResponseDto result = authService.login(new LoginRequestDto(USERNAME, RAW_PASSWORD))
                .await().indefinitely();

            assertNotNull(result.token());
            assertEquals(USERNAME, result.username());
            assertEquals(ROLE, result.role());
            assertEquals(8 * 3600L, result.expiresIn());
        }

        @Test
        @DisplayName("should reject when password does not match")
        void shouldRejectWrongPassword() {
            when(repository.findByUsername(USERNAME)).thenReturn(Uni.createFrom().item(activeUser()));

            assertThrows(NotAuthorizedException.class,
                () -> authService.login(new LoginRequestDto(USERNAME, "wrong-password")).await().indefinitely());
        }

        @Test
        @DisplayName("should reject when user does not exist (constant-time, no early return)")
        void shouldRejectUnknownUser() {
            when(repository.findByUsername("ghost")).thenReturn(Uni.createFrom().nullItem());

            assertThrows(NotAuthorizedException.class,
                () -> authService.login(new LoginRequestDto("ghost", RAW_PASSWORD)).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("should persist a new user with a bcrypt-hashed password")
        void shouldPersistNewUser() {
            when(repository.existsByUsername(USERNAME)).thenReturn(Uni.createFrom().item(false));
            when(repository.persist(any(AppUser.class))).thenReturn(Uni.createFrom().item(activeUser()));

            authService.createUser(USERNAME, RAW_PASSWORD, ROLE).await().indefinitely();

            verify(repository).persist(any(AppUser.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when username already exists")
        void shouldThrowWhenDuplicate() {
            when(repository.existsByUsername(USERNAME)).thenReturn(Uni.createFrom().item(true));

            assertThrows(IllegalArgumentException.class,
                () -> authService.createUser(USERNAME, RAW_PASSWORD, ROLE).await().indefinitely());

            verify(repository, never()).persist(any(AppUser.class));
        }
    }
}
