package br.com.fiap.postech.soat16.fase1.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PaginationDto;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.*;
import br.com.fiap.postech.soat16.fase1.service.CustomerService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerController — Unit Tests")
class CustomerControllerTest {

    @Mock
    private CustomerService service;

    private CustomerController controller;

    private static final UUID FIXED_UUID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    private CustomerResponseDto response;

    @BeforeEach
    void setUp() {
        controller = new CustomerController(service);
        response = new CustomerResponseDto(FIXED_UUID, "John", "Doe", "john.doe@example.com", "5511987654321", "52998224725", "CPF", null, null);
    }

    @Nested
    @DisplayName("GET /v1/customer — findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated list when customers exist")
        void shouldReturnPaginatedListWhenCustomersExist() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            when(pageable.getQ()).thenReturn(null);
            when(pageable.getPage()).thenReturn(0);
            when(pageable.getSize()).thenReturn(10);

            PaginationDto paginationDto = new PaginationDto(0, 10, 1L, 1, false, false);
            PageableResponseDto<CustomerResponseDto> page = new PageableResponseDto<>(List.of(response), paginationDto);

            when(service.findAll(null, 0, 10)).thenReturn(Uni.createFrom().item(page));

            PageableResponseDto<CustomerResponseDto> result = controller.findAll(pageable).await().indefinitely();

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals("John", result.content().getFirst().getFirstName());
            verify(service).findAll(null, 0, 10);
        }

        @Test
        @DisplayName("should return empty page when no customers exist")
        void shouldReturnEmptyPageWhenNoCustomersExist() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            when(pageable.getQ()).thenReturn(null);
            when(pageable.getPage()).thenReturn(0);
            when(pageable.getSize()).thenReturn(10);

            when(service.findAll(null, 0, 10)).thenReturn(Uni.createFrom().item(PageableResponseDto.emptyList()));

            PageableResponseDto<CustomerResponseDto> result = controller.findAll(pageable).await().indefinitely();

            assertTrue(result.content().isEmpty());
        }

        @Test
        @DisplayName("should propagate exception from service")
        void shouldPropagateException() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            when(pageable.getQ()).thenReturn(null);
            when(pageable.getPage()).thenReturn(0);
            when(pageable.getSize()).thenReturn(10);

            when(service.findAll(null, 0, 10))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

            assertThrows(RuntimeException.class, () -> controller.findAll(pageable).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("GET /v1/customer/{id} — findById")
    class FindById {

        @Test
        @DisplayName("should return customer when found")
        void shouldReturnCustomerWhenFound() {
            when(service.findById(FIXED_UUID)).thenReturn(Uni.createFrom().item(response));

            CustomerResponseDto result = controller.findById(FIXED_UUID).await().indefinitely();

            assertNotNull(result);
            assertEquals("John", result.getFirstName());
            verify(service).findById(FIXED_UUID);
        }

        @Test
        @DisplayName("should propagate CustomerNotFoundException")
        void shouldPropagateNotFoundException() {
            when(service.findById(FIXED_UUID))
                    .thenReturn(Uni.createFrom().failure(new CustomerNotFoundException()));

            assertThrows(CustomerNotFoundException.class,
                    () -> controller.findById(FIXED_UUID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("POST /v1/customer — create")
    class Create {

        @Test
        @DisplayName("should return HTTP 201 when create succeeds")
        void shouldReturn201WhenCreateSucceeds() {
            CustomerRequestDto dto = new CustomerRequestDto("John", "Doe", "john.doe@example.com", "5511987654321", "529.982.247-25");

            when(service.create(dto)).thenReturn(Uni.createFrom().voidItem());

            Response result = controller.create(dto).await().indefinitely();

            assertEquals(201, result.getStatus());
            verify(service).create(dto);
        }

        @Test
        @DisplayName("should propagate DuplicateDocumentException")
        void shouldPropagateDuplicateDocumentException() {
            CustomerRequestDto dto = new CustomerRequestDto("John", "Doe", "john.doe@example.com", "5511987654321", "529.982.247-25");

            when(service.create(dto))
                    .thenReturn(Uni.createFrom().failure(new DuplicateDocumentException()));

            assertThrows(DuplicateDocumentException.class,
                    () -> controller.create(dto).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("PUT /v1/customer/{id} — update")
    class Update {

        @Test
        @DisplayName("should return HTTP 200 with updated body")
        void shouldReturn200WithUpdatedBody() {
            CustomerRequestDto dto = new CustomerRequestDto("Jane", "Doe", "jane.doe@example.com", "5511987654321", "529.982.247-25");
            CustomerResponseDto updated = new CustomerResponseDto(FIXED_UUID, "Jane", "Doe", "jane.doe@example.com", "5511987654321",
                    "52998224725", "CPF", OffsetDateTime.now(), OffsetDateTime.now());

            when(service.update(FIXED_UUID, dto)).thenReturn(Uni.createFrom().item(updated));

            Response result = controller.update(FIXED_UUID, dto).await().indefinitely();

            assertEquals(200, result.getStatus());
            verify(service).update(FIXED_UUID, dto);
        }

        @Test
        @DisplayName("should propagate CustomerNotFoundException")
        void shouldPropagateNotFoundException() {
            CustomerRequestDto dto = new CustomerRequestDto("Jane", "Doe", "jane.doe@example.com", "5511987654321", "529.982.247-25");

            when(service.update(FIXED_UUID, dto))
                    .thenReturn(Uni.createFrom().failure(new CustomerNotFoundException()));

            assertThrows(CustomerNotFoundException.class,
                    () -> controller.update(FIXED_UUID, dto).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("DELETE /v1/customer/{id} — delete")
    class Delete {

        @Test
        @DisplayName("should return HTTP 204 when delete succeeds")
        void shouldReturn204WhenDeleteSucceeds() {
            when(service.delete(FIXED_UUID)).thenReturn(Uni.createFrom().voidItem());

            Response result = controller.delete(FIXED_UUID).await().indefinitely();

            assertEquals(204, result.getStatus());
            verify(service).delete(FIXED_UUID);
        }

        @Test
        @DisplayName("should propagate CustomerNotFoundException")
        void shouldPropagateNotFoundException() {
            when(service.delete(FIXED_UUID))
                    .thenReturn(Uni.createFrom().failure(new CustomerNotFoundException()));

            assertThrows(CustomerNotFoundException.class,
                    () -> controller.delete(FIXED_UUID).await().indefinitely());
        }
    }
}
