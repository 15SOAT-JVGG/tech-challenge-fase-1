package br.com.fiap.postech.soat16.fase1.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.auth.application.command.LoginCommand;
import br.com.fiap.postech.soat16.fase1.auth.application.port.out.AppUserPersistencePort;
import br.com.fiap.postech.soat16.fase1.auth.application.port.out.AuthPasswordPort;
import br.com.fiap.postech.soat16.fase1.auth.application.port.out.JwtTokenPort;
import br.com.fiap.postech.soat16.fase1.auth.application.result.IssuedToken;
import br.com.fiap.postech.soat16.fase1.auth.application.result.LoginResult;
import br.com.fiap.postech.soat16.fase1.auth.domain.exception.InvalidCredentialsException;
import br.com.fiap.postech.soat16.fase1.auth.domain.model.AppUser;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Unit Tests")
class AuthServiceTest {

    private static final String USERNAME = "admin";
    private static final String RAW_PASSWORD = "S3nh@-F0rte";
    private static final String HASHED_PASSWORD = "$2a$10$hash";
    private static final String ROLE = "ADMIN";
    private static final long EXPIRATION_SECONDS = 8 * 3600L;

    @Mock
    private AppUserPersistencePort repository;

    @Mock
    private AuthPasswordPort password;

    @Mock
    private JwtTokenPort token;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(repository, password, token);
    }

    private AppUser activeUser() {
        return new AppUser(USERNAME, HASHED_PASSWORD, ROLE);
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("issues a JWT with the user's role when credentials are valid")
        void shouldLoginWithValidCredentials() {
            when(repository.findByUsername(USERNAME))
                    .thenReturn(Uni.createFrom().item(activeUser()));
            when(password.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(token.issue(USERNAME, ROLE))
                    .thenReturn(new IssuedToken("jwt", EXPIRATION_SECONDS));

            LoginResult result = authService.login(new LoginCommand(USERNAME, RAW_PASSWORD))
                    .await().indefinitely();

            assertEquals("jwt", result.token());
            assertEquals(USERNAME, result.username());
            assertEquals(ROLE, result.role());
            assertEquals(EXPIRATION_SECONDS, result.expiresIn());
        }

        @Test
        @DisplayName("rejects when password does not match")
        void shouldRejectWrongPassword() {
            when(repository.findByUsername(USERNAME))
                    .thenReturn(Uni.createFrom().item(activeUser()));
            when(password.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

            assertThrows(
                    InvalidCredentialsException.class,
                    () -> authService.login(new LoginCommand(USERNAME, RAW_PASSWORD))
                            .await().indefinitely());
        }

        @Test
        @DisplayName("rejects an unknown user without skipping password verification")
        void shouldRejectUnknownUser() {
            when(repository.findByUsername("ghost")).thenReturn(Uni.createFrom().nullItem());
            when(password.matches(eq(RAW_PASSWORD), anyString())).thenReturn(false);

            assertThrows(
                    InvalidCredentialsException.class,
                    () -> authService.login(new LoginCommand("ghost", RAW_PASSWORD))
                            .await().indefinitely());

            verify(password).matches(eq(RAW_PASSWORD), anyString());
        }
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("persists a new user with a hashed password")
        void shouldPersistNewUser() {
            when(repository.existsByUsername(USERNAME)).thenReturn(Uni.createFrom().item(false));
            when(password.hash(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(repository.save(any(AppUser.class)))
                    .thenReturn(Uni.createFrom().item(activeUser()));

            authService.createUser(USERNAME, RAW_PASSWORD, ROLE).await().indefinitely();

            verify(repository).save(any(AppUser.class));
        }

        @Test
        @DisplayName("rejects an existing username")
        void shouldThrowWhenDuplicate() {
            when(repository.existsByUsername(USERNAME)).thenReturn(Uni.createFrom().item(true));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.createUser(USERNAME, RAW_PASSWORD, ROLE)
                            .await().indefinitely());

            verify(repository, never()).save(any(AppUser.class));
        }
    }
}
