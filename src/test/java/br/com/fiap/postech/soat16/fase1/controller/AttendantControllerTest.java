package br.com.fiap.postech.soat16.fase1.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import br.com.fiap.postech.soat16.fase1.dto.pagination.*;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantResponseDto;
import br.com.fiap.postech.soat16.fase1.service.AttendantService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendantController - Unit Tests")
class AttendantControllerTest {

    @Mock
    private AttendantService service;

    private AttendantController controller;

    private static final UUID FIXED_UUID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    private AttendantResponseDto response;

    @BeforeEach
    void setUp() {
        controller = new AttendantController(service);
        response = new AttendantResponseDto(FIXED_UUID, "Ana", "Silva", "ana@example.com", "5511999999999", true, null);
    }

    @Nested
    @DisplayName("GET /v1/attendant")
    class FindAll {

        @Test
        @DisplayName("should return paginated list")
        void shouldReturnPaginatedList() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            PaginationDto pagination = new PaginationDto(0, 10, 1L, 1, false, false);
            PageableResponseDto<AttendantResponseDto> page = new PageableResponseDto<>(List.of(response), pagination);

            when(pageable.getQ()).thenReturn(null);
            when(pageable.getPage()).thenReturn(0);
            when(pageable.getSize()).thenReturn(10);
            when(service.findAll(null, 0, 10)).thenReturn(page);

            var result = controller.findAll(pageable);

            assertEquals(1, result.content().size());
            verify(service).findAll(null, 0, 10);
        }
    }

    @Test
    @DisplayName("should return attendant by id")
    void shouldReturnAttendantById() {
        when(service.findById(FIXED_UUID)).thenReturn(response);

        AttendantResponseDto result = controller.findById(FIXED_UUID);

        assertEquals(FIXED_UUID, result.attendantId());
        verify(service).findById(FIXED_UUID);
    }

    @Test
    @DisplayName("should return HTTP 201 when create succeeds")
    void shouldReturn201WhenCreateSucceeds() {
        AttendantRequestDto request = new AttendantRequestDto(
                "Ana", "Silva", "ana@example.com", "5511999999999", "password123");

        Response result = controller.create(request);

        assertEquals(201, result.getStatus());
        verify(service).create(request);
    }

    @Test
    @DisplayName("should return login response")
    void shouldReturnLoginResponse() {
        AttendantLoginRequestDto request = new AttendantLoginRequestDto("ana@example.com", "password123");
        AttendantLoginResponseDto loginResponse = new AttendantLoginResponseDto(FIXED_UUID, "Ana", "Silva", "ana@example.com", true);

        when(service.login(request)).thenReturn(loginResponse);

        AttendantLoginResponseDto result = controller.login(request);

        assertNotNull(result);
        assertEquals(FIXED_UUID, result.attendantId());
    }

    @Test
    @DisplayName("should return HTTP 200 when update succeeds")
    void shouldReturn200WhenUpdateSucceeds() {
        AttendantRequestDto request = new AttendantRequestDto(
                "Maria", "Souza", "maria@example.com", "5511888888888", "1234");

        when(service.update(FIXED_UUID, request)).thenReturn(response);

        Response result = controller.update(FIXED_UUID, request);

        assertEquals(200, result.getStatus());
        verify(service).update(FIXED_UUID, request);
    }

    @Test
    @DisplayName("should return HTTP 204 when delete succeeds")
    void shouldReturn204WhenDeleteSucceeds() {
        Response result = controller.delete(FIXED_UUID);

        assertEquals(204, result.getStatus());
        verify(service).delete(FIXED_UUID);
    }
}
