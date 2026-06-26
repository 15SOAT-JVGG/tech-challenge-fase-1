package br.com.fiap.postech.soat16.fase1.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.dto.request.WorkerLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkerLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkerResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateWorkerEmailException;
import br.com.fiap.postech.soat16.fase1.exception.InactiveWorkerException;
import br.com.fiap.postech.soat16.fase1.exception.InvalidWorkerCredentialsException;
import br.com.fiap.postech.soat16.fase1.exception.WorkerNotFoundException;
import br.com.fiap.postech.soat16.fase1.mapper.WorkerMapper;
import br.com.fiap.postech.soat16.fase1.model.Worker;
import br.com.fiap.postech.soat16.fase1.model.enums.WorkerProfile;
import br.com.fiap.postech.soat16.fase1.repository.WorkerRepository;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerService - Unit Tests")
class WorkerServiceTest {

    @Mock
    private WorkerRepository repository;

    @Mock
    private WorkerMapper mapper;

    @Mock
    private PasswordService passwordService;

    private WorkerService service;

    private static final UUID FIXED_UUID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    private Worker entity;
    private WorkerResponseDto response;

    @BeforeEach
    void setUp() {
        service = new WorkerService(repository, mapper, passwordService);
        entity = new Worker(FIXED_UUID, WorkerProfile.MECHANIC, "Ana", "Silva", "ana@example.com", "5511999999999", "hash", true);
        response = new WorkerResponseDto(FIXED_UUID, "Ana", "Silva", "ana@example.com", "5511999999999", true, null);
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated response")
        void shouldReturnPaginatedResponse() {
            when(repository.findPage(0, 10)).thenReturn(Uni.createFrom().item(List.of(entity)));
            when(repository.count()).thenReturn(Uni.createFrom().item(1L));
            when(mapper.toResponse(entity)).thenReturn(response);

            var result = service.findAll(null, 0, 10).await().indefinitely();

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals(1L, result.pagination().totalElements());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return worker response when found")
        void shouldReturnWorkerWhenFound() {
            when(repository.findByWorkerId(FIXED_UUID)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            WorkerResponseDto result = service.findById(FIXED_UUID).await().indefinitely();

            assertEquals(FIXED_UUID, result.workerId());
        }

        @Test
        @DisplayName("should throw WorkerNotFoundException when not found")
        void shouldThrowNotFoundWhenMissing() {
            when(repository.findByWorkerId(FIXED_UUID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(WorkerNotFoundException.class,
                    () -> service.findById(FIXED_UUID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should persist entity when email is unique")
        void shouldPersistWhenEmailIsUnique() {
            WorkerRequestDto request = new WorkerRequestDto(
                    "Ana", "Silva", "ana@example.com", "5511999999999", "password123", WorkerProfile.MECHANIC);

            when(repository.existsByEmail("ana@example.com")).thenReturn(Uni.createFrom().item(false));
            when(passwordService.hash("password123")).thenReturn("hash");
            when(mapper.toEntity(request, "hash")).thenReturn(entity);
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));

            assertDoesNotThrow(() -> service.create(request).await().indefinitely());

            verify(repository).persist(entity);
        }

        @Test
        @DisplayName("should throw DuplicateWorkerEmailException when email exists")
        void shouldThrowDuplicateWhenEmailExists() {
            WorkerRequestDto request = new WorkerRequestDto(
                    "Ana", "Silva", "ana@example.com", "5511999999999", "password123", WorkerProfile.MECHANIC);

            when(repository.existsByEmail("ana@example.com")).thenReturn(Uni.createFrom().item(true));

            assertThrows(DuplicateWorkerEmailException.class,
                    () -> service.create(request).await().indefinitely());
            verify(repository, never()).persist(any(Worker.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update entity and return response")
        void shouldUpdateAndReturn() {
            WorkerRequestDto request = new WorkerRequestDto(
                    "Maria", "Souza", "maria@example.com", "5511888888888", "1234", WorkerProfile.MECHANIC);

            when(repository.findByWorkerId(FIXED_UUID)).thenReturn(Uni.createFrom().item(entity));
            when(repository.existsByEmailAndDifferentId("maria@example.com", FIXED_UUID))
                    .thenReturn(Uni.createFrom().item(false));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            WorkerResponseDto result = service.update(FIXED_UUID, request).await().indefinitely();

            assertNotNull(result);
            verify(mapper).updateEntity(entity, request);
            verify(repository).persist(entity);
        }

        @Test
        @DisplayName("should throw DuplicateWorkerEmailException when another worker has email")
        void shouldThrowDuplicateWhenAnotherWorkerHasEmail() {
            WorkerRequestDto request = new WorkerRequestDto(
                    "Maria", "Souza", "maria@example.com", "5511888888888", "1234", WorkerProfile.MECHANIC);

            when(repository.findByWorkerId(FIXED_UUID)).thenReturn(Uni.createFrom().item(entity));
            when(repository.existsByEmailAndDifferentId("maria@example.com", FIXED_UUID))
                    .thenReturn(Uni.createFrom().item(true));

            assertThrows(DuplicateWorkerEmailException.class,
                    () -> service.update(FIXED_UUID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should throw WorkerNotFoundException when worker is missing")
        void shouldThrowNotFoundWhenMissing() {
            WorkerRequestDto request = new WorkerRequestDto(
                    "Maria", "Souza", "maria@example.com", "5511888888888", "1234", WorkerProfile.MECHANIC);

            when(repository.findByWorkerId(FIXED_UUID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(WorkerNotFoundException.class,
                    () -> service.update(FIXED_UUID, request).await().indefinitely());
            verify(repository, never()).persist(any(Worker.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete worker when found")
        void shouldDeleteWhenFound() {
            when(repository.deleteByWorkerId(FIXED_UUID)).thenReturn(Uni.createFrom().item(1L));

            assertDoesNotThrow(() -> service.delete(FIXED_UUID).await().indefinitely());
        }

        @Test
        @DisplayName("should throw WorkerNotFoundException when no record deleted")
        void shouldThrowWhenNoRecordDeleted() {
            when(repository.deleteByWorkerId(FIXED_UUID)).thenReturn(Uni.createFrom().item(0L));

            assertThrows(WorkerNotFoundException.class,
                    () -> service.delete(FIXED_UUID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should return login response when credentials are valid")
        void shouldReturnLoginResponseWhenCredentialsAreValid() {
            WorkerLoginRequestDto request = new WorkerLoginRequestDto("ana@example.com", "password123");
            WorkerLoginResponseDto loginResponse = new WorkerLoginResponseDto(FIXED_UUID, "Ana", "Silva", "ana@example.com", true);

            when(repository.findByEmail("ana@example.com")).thenReturn(Uni.createFrom().item(entity));
            when(passwordService.matches("password123", "hash")).thenReturn(true);
            when(mapper.toLoginResponse(entity)).thenReturn(loginResponse);

            WorkerLoginResponseDto result = service.login(request).await().indefinitely();

            assertEquals(FIXED_UUID, result.workerId());
        }

        @Test
        @DisplayName("should throw InvalidWorkerCredentialsException when email is missing")
        void shouldThrowInvalidCredentialsWhenEmailIsMissing() {
            WorkerLoginRequestDto request = new WorkerLoginRequestDto("ana@example.com", "password123");

            when(repository.findByEmail("ana@example.com")).thenReturn(Uni.createFrom().nullItem());

            assertThrows(InvalidWorkerCredentialsException.class,
                    () -> service.login(request).await().indefinitely());
        }

        @Test
        @DisplayName("should throw InactiveWorkerException when worker is inactive")
        void shouldThrowInactiveWhenWorkerIsInactive() {
            WorkerLoginRequestDto request = new WorkerLoginRequestDto("ana@example.com", "password123");
            entity.setActive(false);

            when(repository.findByEmail("ana@example.com")).thenReturn(Uni.createFrom().item(entity));

            assertThrows(InactiveWorkerException.class,
                    () -> service.login(request).await().indefinitely());
        }

        @Test
        @DisplayName("should throw InvalidWorkerCredentialsException when password is invalid")
        void shouldThrowInvalidCredentialsWhenPasswordIsInvalid() {
            WorkerLoginRequestDto request = new WorkerLoginRequestDto("ana@example.com", "password123");

            when(repository.findByEmail("ana@example.com")).thenReturn(Uni.createFrom().item(entity));
            when(passwordService.matches("password123", "hash")).thenReturn(false);

            assertThrows(InvalidWorkerCredentialsException.class,
                    () -> service.login(request).await().indefinitely());
        }
    }
}
