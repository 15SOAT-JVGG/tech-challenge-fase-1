package br.com.fiap.postech.soat16.fase1.controller;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderMetricsResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderServiceResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.error.ApiErrorResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.ErrorType;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Part;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.enums.DocumentType;
import br.com.fiap.postech.soat16.fase1.model.enums.EstimateStatus;
import br.com.fiap.postech.soat16.fase1.model.enums.VehicleType;
import br.com.fiap.postech.soat16.fase1.model.enums.WorkOrderStatus;
import br.com.fiap.postech.soat16.fase1.repository.CustomerRepository;
import br.com.fiap.postech.soat16.fase1.repository.PartRepository;
import br.com.fiap.postech.soat16.fase1.repository.VehicleRepository;
import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;

/**
 * Testes de integração HTTP do fluxo completo de ordem de serviço contra um PostgreSQL real
 * (Testcontainers via {@link PostgresTestResource}). Exercita toda a stack: RestAssured →
 * WorkOrderController → WorkOrderService → repositórios → Hibernate Reactive → PostgreSQL.
 * Cada teste semeia seu próprio cliente/veículo/peça, já que o schema é drop-and-create por
 * execução (não por método).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("WorkOrderController — Integration Tests (HTTP)")
class WorkOrderControllerIT {

    private static final String WORK_ORDERS_PATH = "/v1/work-orders";
    private static final AtomicInteger PLATE_COUNTER = new AtomicInteger();

    @Inject
    CustomerRepository customerRepository;

    @Inject
    VehicleRepository vehicleRepository;

    @Inject
    PartRepository partRepository;

    // ---------------------------------------------------------------------
    // Autenticação — as APIs administrativas exigem JWT; assinamos um token
    // (ADMIN + MECHANIC) com a mesma chave/issuer da aplicação e o aplicamos a
    // todas as requisições. Endpoints públicos ignoram o header.
    // ---------------------------------------------------------------------

    @BeforeAll
    static void authenticateAllRequests() {
        String token = Jwt.issuer("oficina-api")
                .upn("integration-test")
                .groups(Set.of("ADMIN", "MECHANIC"))
                .expiresIn(Duration.ofHours(1))
                .sign();
        RestAssured.filters((requestSpec, responseSpec, ctx) -> {
            requestSpec.header("Authorization", "Bearer " + token);
            return ctx.next(requestSpec, responseSpec);
        });
    }

    @AfterAll
    static void clearAuthentication() {
        RestAssured.replaceFiltersWith(java.util.List.of());
    }

    // ---------------------------------------------------------------------
    // Seeding helpers
    // ---------------------------------------------------------------------

    // Persiste fora do event loop, estabelecendo o contexto Vert.x exigido pelo Hibernate
    // Reactive (mesma abordagem do DataSeeder no startup / AuthControllerIT).
    private <T> T persistInTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable e) {
            throw new IllegalStateException("Falha ao preparar dados de teste", e);
        }
    }

    private static String uniqueLicensePlate() {
        int n = PLATE_COUNTER.incrementAndGet() % 10_000;
        return "ABC" + String.format("%04d", n);
    }

    private UUID seedCustomer() {
        Customer customer = new Customer();
        customer.setFirstName("Maria");
        customer.setLastName("Silva");
        customer.setPhoneNumber("+5511999999999");
        customer.setDocument("DOC-" + UUID.randomUUID());
        customer.setDocumentType(DocumentType.CPF);
        return persistInTransaction(() -> customerRepository.persist(customer)).getId();
    }

    private UUID seedVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(uniqueLicensePlate());
        vehicle.setManufacturer("Fiat");
        vehicle.setModel("Uno");
        vehicle.setColor("Branco");
        vehicle.setYear(2020);
        vehicle.setKmDriven(50_000L);
        vehicle.setType(VehicleType.CAR);
        return persistInTransaction(() -> vehicleRepository.persist(vehicle)).getId();
    }

    private UUID seedPart(BigDecimal unitPrice) {
        Part part = new Part("Filtro de óleo", "Filtro de óleo padrão", unitPrice, 100, "UN");
        return persistInTransaction(() -> partRepository.persist(part)).getId();
    }

    // ---------------------------------------------------------------------
    // Flow helpers (drive the work order through the API, like a real client)
    // ---------------------------------------------------------------------

    private UUID createWorkOrder(UUID customerId, UUID vehicleId) {
        String body = """
                {"customerId":"%s","vehicleId":"%s","description":"Revisão geral"}
                """.formatted(customerId, vehicleId);

        given()
                .contentType("application/json")
                .body(body)
        .when()
                .post(WORK_ORDERS_PATH)
        .then()
                .statusCode(201);

        return findWorkOrderIdByVehicle(vehicleId);
    }

    // POST /work-orders não retorna o id criado (apenas 201 sem corpo); localizamos pelo
    // veículo, que é único por teste, usando só a API pública (sem tocar no repositório).
    private UUID findWorkOrderIdByVehicle(UUID vehicleId) {
        PageableResponseDto<WorkOrderResponseDto> page = given()
                .queryParam("size", 100)
        .when()
                .get(WORK_ORDERS_PATH)
        .then()
                .statusCode(200)
                .extract().as(new TypeRef<>() {
                });

        return page.content().stream()
                .filter(workOrder -> vehicleId.equals(workOrder.vehicleId()))
                .map(WorkOrderResponseDto::workOrderId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Work order not found for vehicle " + vehicleId));
    }

    private WorkOrderResponseDto getWorkOrder(UUID workOrderId) {
        return given()
        .when()
                .get(WORK_ORDERS_PATH + "/" + workOrderId)
        .then()
                .statusCode(200)
                .extract().as(WorkOrderResponseDto.class);
    }

    private void updateStatus(UUID workOrderId, WorkOrderStatus status) {
        given()
                .contentType("application/json")
                .body("{\"status\":\"" + status + "\"}")
        .when()
                .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/status")
        .then()
                .statusCode(200);
    }

    private EstimateResponseDto createEstimate(UUID workOrderId, UUID partId, int quantity) {
        String body = """
                {"items":[{"partId":"%s","quantity":%d}]}
                """.formatted(partId, quantity);

        return given()
                .contentType("application/json")
                .body(body)
        .when()
                .post(WORK_ORDERS_PATH + "/" + workOrderId + "/estimate")
        .then()
                .statusCode(201)
                .extract().as(EstimateResponseDto.class);
    }

    private EstimateResponseDto approveEstimate(UUID workOrderId, UUID estimateId) {
        return given()
        .when()
                .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/estimate/" + estimateId + "/approve")
        .then()
                .statusCode(200)
                .extract().as(EstimateResponseDto.class);
    }

    private EstimateResponseDto rejectEstimate(UUID workOrderId, UUID estimateId) {
        return given()
        .when()
                .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/estimate/" + estimateId + "/reject")
        .then()
                .statusCode(200)
                .extract().as(EstimateResponseDto.class);
    }

    private WorkOrderServiceResponseDto addService(UUID workOrderId, String description, BigDecimal price) {
        return given()
                .contentType("application/json")
                .body("{\"description\":\"%s\",\"price\":%s}".formatted(description, price))
        .when()
                .post(WORK_ORDERS_PATH + "/" + workOrderId + "/services")
        .then()
                .statusCode(201)
                .extract().as(WorkOrderServiceResponseDto.class);
    }

    // Lê o estoque atual da peça direto do repositório (estabelecendo o contexto Vert.x exigido
    // pelo Hibernate Reactive), já que não há endpoint de leitura de peça neste teste.
    private int stockOf(UUID partId) {
        return persistInTransaction(() -> partRepository.find("id = ?1", partId).firstResult()).getStockQuantity();
    }

    // Leva a ordem até IN_PROGRESS com um orçamento aprovado, igual ao fluxo documentado no
    // WORKORDER.md: DIAGNOSIS -> WAITING_APPROVAL -> (orçamento aprovado) -> IN_PROGRESS.
    private UUID createWorkOrderInProgress(BigDecimal unitPrice, int quantity) {
        UUID customerId = seedCustomer();
        UUID vehicleId = seedVehicle();
        UUID partId = seedPart(unitPrice);
        UUID workOrderId = createWorkOrder(customerId, vehicleId);

        updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
        updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
        EstimateResponseDto estimate = createEstimate(workOrderId, partId, quantity);
        approveEstimate(workOrderId, estimate.estimateId());
        updateStatus(workOrderId, WorkOrderStatus.IN_PROGRESS);

        return workOrderId;
    }

    private ApiErrorResponseDto extractError(Response response) {
        return response.as(ApiErrorResponseDto.class);
    }

    @Nested
    @DisplayName("Fluxo completo da ordem de serviço")
    class FullLifecycle {

        @Test
        @DisplayName("deve percorrer RECEIVED -> DIAGNOSIS -> WAITING_APPROVAL -> APPROVED -> IN_PROGRESS -> COMPLETED -> DELIVERED")
        void shouldCompleteFullLifecycle() {
            UUID customerId = seedCustomer();
            UUID vehicleId = seedVehicle();
            UUID partId = seedPart(new BigDecimal("150.00"));

            UUID workOrderId = createWorkOrder(customerId, vehicleId);

            WorkOrderResponseDto created = getWorkOrder(workOrderId);
            assertEquals(WorkOrderStatus.RECEIVED, created.status());
            assertNotNull(created.openedAt());
            assertNull(created.closedAt());

            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            assertEquals(WorkOrderStatus.DIAGNOSIS, getWorkOrder(workOrderId).status());

            updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
            assertEquals(WorkOrderStatus.WAITING_APPROVAL, getWorkOrder(workOrderId).status());

            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 2);
            assertEquals(EstimateStatus.PENDING, estimate.status());
            assertEquals(0, new BigDecimal("300.00").compareTo(estimate.totalAmount()));

            EstimateResponseDto approved = approveEstimate(workOrderId, estimate.estimateId());
            assertEquals(EstimateStatus.APPROVED, approved.status());

            WorkOrderResponseDto afterApproval = getWorkOrder(workOrderId);
            assertEquals(WorkOrderStatus.APPROVED, afterApproval.status());
            assertEquals(0, new BigDecimal("300.00").compareTo(afterApproval.estimatedValue()));

            WorkOrderServiceResponseDto service = given()
                    .contentType("application/json")
                    .body("{\"description\":\"Troca de óleo\",\"price\":120.00}")
            .when()
                    .post(WORK_ORDERS_PATH + "/" + workOrderId + "/services")
            .then()
                    .statusCode(201)
                    .extract().as(WorkOrderServiceResponseDto.class);
            assertEquals("Troca de óleo", service.description());

            updateStatus(workOrderId, WorkOrderStatus.IN_PROGRESS);
            assertEquals(WorkOrderStatus.IN_PROGRESS, getWorkOrder(workOrderId).status());

            WorkOrderResponseDto closed = given()
                    .contentType("application/json")
                    .body("{\"finalValue\":410.50}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/close")
            .then()
                    .statusCode(200)
                    .extract().as(WorkOrderResponseDto.class);
            assertEquals(WorkOrderStatus.COMPLETED, closed.status());
            assertEquals(0, new BigDecimal("410.50").compareTo(closed.finalValue()));
            assertNotNull(closed.closedAt());

            updateStatus(workOrderId, WorkOrderStatus.DELIVERED);
            assertEquals(WorkOrderStatus.DELIVERED, getWorkOrder(workOrderId).status());
        }

        @Test
        @DisplayName("deve cancelar a partir de um status não terminal")
        void shouldCancelFromNonTerminalStatus() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            updateStatus(workOrderId, WorkOrderStatus.CANCELLED);

            assertEquals(WorkOrderStatus.CANCELLED, getWorkOrder(workOrderId).status());
        }

        @Test
        @DisplayName("deve travar a ordem para qualquer alteração depois de DELIVERED")
        void shouldLockWorkOrderAfterDelivered() {
            UUID workOrderId = createWorkOrderInProgress(new BigDecimal("200.00"), 1);

            given()
                    .contentType("application/json")
                    .body("{}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/close")
            .then()
                    .statusCode(200);

            updateStatus(workOrderId, WorkOrderStatus.DELIVERED);

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body("{\"status\":\"CANCELLED\"}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/status")
            .then()
                    .statusCode(422)
                    .extract().response());
            assertEquals(ErrorType.BUSINESS, error.type());
            assertEquals("WORK_ORDER_LOCKED", error.code());
        }
    }

    @Nested
    @DisplayName("POST /v1/work-orders — create")
    class Create {

        @Test
        @DisplayName("deve criar a ordem com status RECEIVED")
        void shouldCreateWorkOrder() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            assertEquals(WorkOrderStatus.RECEIVED, getWorkOrder(workOrderId).status());
        }

        @Test
        @DisplayName("deve retornar 404 quando o cliente não existe")
        void shouldReturn404WhenCustomerNotFound() {
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":"Revisão"}
                    """.formatted(UUID.randomUUID(), seedVehicle());

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body(body)
            .when()
                    .post(WORK_ORDERS_PATH)
            .then()
                    .statusCode(404)
                    .extract().response());
            assertEquals("CUSTOMER_NOT_FOUND", error.code());
        }

        @Test
        @DisplayName("deve retornar 404 quando o veículo não existe")
        void shouldReturn404WhenVehicleNotFound() {
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":"Revisão"}
                    """.formatted(seedCustomer(), UUID.randomUUID());

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body(body)
            .when()
                    .post(WORK_ORDERS_PATH)
            .then()
                    .statusCode(404)
                    .extract().response());
            assertEquals("VEHICLE_NOT_FOUND", error.code());
        }

        @Test
        @DisplayName("deve retornar 400 quando a descrição está em branco")
        void shouldReturn400WhenDescriptionBlank() {
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":""}
                    """.formatted(seedCustomer(), seedVehicle());

            given()
                    .contentType("application/json")
                    .body(body)
            .when()
                    .post(WORK_ORDERS_PATH)
            .then()
                    .statusCode(400);
        }
    }

    @Nested
    @DisplayName("PATCH /v1/work-orders/{id}/status — updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("deve retornar 404 quando a ordem não existe")
        void shouldReturn404WhenWorkOrderNotFound() {
            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body("{\"status\":\"DIAGNOSIS\"}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + UUID.randomUUID() + "/status")
            .then()
                    .statusCode(404)
                    .extract().response());
            assertEquals("WORK_ORDER_NOT_FOUND", error.code());
        }

        @Test
        @DisplayName("deve rejeitar pular etapas (RECEIVED -> APPROVED)")
        void shouldRejectSkippingStages() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body("{\"status\":\"APPROVED\"}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/status")
            .then()
                    .statusCode(422)
                    .extract().response());
            assertEquals("INVALID_STATUS_TRANSITION", error.code());
        }

        @Test
        @DisplayName("deve rejeitar ir direto para COMPLETED pelo endpoint genérico")
        void shouldRejectCompletedViaGenericEndpoint() {
            UUID workOrderId = createWorkOrderInProgress(new BigDecimal("100.00"), 1);

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body("{\"status\":\"COMPLETED\"}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/status")
            .then()
                    .statusCode(422)
                    .extract().response());
            assertEquals("INVALID_STATUS_TRANSITION", error.code());
        }

        @Test
        @DisplayName("deve rejeitar aprovar sem orçamento aprovado")
        void shouldRejectApprovingWithoutApprovedEstimate() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body("{\"status\":\"APPROVED\"}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/status")
            .then()
                    .statusCode(422)
                    .extract().response());
            assertEquals("ESTIMATE_NOT_APPROVED", error.code());
        }
    }

    @Nested
    @DisplayName("POST /v1/work-orders/{id}/estimate — createEstimate")
    class CreateEstimate {

        @Test
        @DisplayName("deve criar o orçamento com total calculado a partir dos itens")
        void shouldCreateEstimate() {
            UUID partId = seedPart(new BigDecimal("50.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 3);

            assertEquals(EstimateStatus.PENDING, estimate.status());
            assertEquals(0, new BigDecimal("150.00").compareTo(estimate.totalAmount()));
            assertEquals(1, estimate.items().size());
        }

        @Test
        @DisplayName("deve retornar 404 quando a peça não existe")
        void shouldReturn404WhenPartNotFound() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            String body = """
                    {"items":[{"partId":"%s","quantity":1}]}
                    """.formatted(UUID.randomUUID());

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body(body)
            .when()
                    .post(WORK_ORDERS_PATH + "/" + workOrderId + "/estimate")
            .then()
                    .statusCode(404)
                    .extract().response());
            assertEquals("ESTIMATE_PART_NOT_FOUND", error.code());
        }

        @Test
        @DisplayName("deve retornar 400 quando a lista de itens está vazia")
        void shouldReturn400WhenItemsEmpty() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            given()
                    .contentType("application/json")
                    .body("{\"items\":[]}")
            .when()
                    .post(WORK_ORDERS_PATH + "/" + workOrderId + "/estimate")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("deve retornar 422 quando a ordem está cancelada")
        void shouldReturn422WhenWorkOrderLocked() {
            UUID partId = seedPart(new BigDecimal("50.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.CANCELLED);

            String body = """
                    {"items":[{"partId":"%s","quantity":1}]}
                    """.formatted(partId);

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body(body)
            .when()
                    .post(WORK_ORDERS_PATH + "/" + workOrderId + "/estimate")
            .then()
                    .statusCode(422)
                    .extract().response());
            assertEquals("WORK_ORDER_LOCKED", error.code());
        }
    }

    @Nested
    @DisplayName("PATCH /v1/work-orders/{id}/estimate/{estimateId}/approve — approveEstimate")
    class ApproveEstimate {

        @Test
        @DisplayName("deve aprovar o orçamento e avançar a ordem para APPROVED")
        void shouldApproveAndAdvanceStatus() {
            UUID partId = seedPart(new BigDecimal("80.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 1);

            EstimateResponseDto approved = approveEstimate(workOrderId, estimate.estimateId());

            assertEquals(EstimateStatus.APPROVED, approved.status());
            assertNotNull(approved.approvedAt());
            assertEquals(WorkOrderStatus.APPROVED, getWorkOrder(workOrderId).status());
        }

        @Test
        @DisplayName("deve retornar 404 quando o orçamento não existe")
        void shouldReturn404WhenEstimateNotFound() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            ApiErrorResponseDto error = extractError(given()
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/estimate/" + UUID.randomUUID() + "/approve")
            .then()
                    .statusCode(404)
                    .extract().response());
            assertEquals("ESTIMATE_NOT_FOUND", error.code());
        }

        @Test
        @DisplayName("deve retornar 409 quando o orçamento já foi decidido")
        void shouldReturn409WhenAlreadyDecided() {
            UUID partId = seedPart(new BigDecimal("80.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 1);
            approveEstimate(workOrderId, estimate.estimateId());

            ApiErrorResponseDto error = extractError(given()
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/estimate/" + estimate.estimateId() + "/approve")
            .then()
                    .statusCode(409)
                    .extract().response());
            assertEquals("ESTIMATE_ALREADY_DECIDED", error.code());
        }
    }

    @Nested
    @DisplayName("POST /v1/work-orders/{id}/services — addService")
    class AddService {

        @Test
        @DisplayName("deve registrar uma linha de mão de obra")
        void shouldAddService() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            WorkOrderServiceResponseDto service = given()
                    .contentType("application/json")
                    .body("{\"description\":\"Alinhamento\",\"price\":80.00}")
            .when()
                    .post(WORK_ORDERS_PATH + "/" + workOrderId + "/services")
            .then()
                    .statusCode(201)
                    .extract().as(WorkOrderServiceResponseDto.class);

            assertEquals("Alinhamento", service.description());
            assertEquals(0, new BigDecimal("80.00").compareTo(service.price()));
        }

        @Test
        @DisplayName("deve retornar 400 quando o preço é zero")
        void shouldReturn400WhenPriceIsZero() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            given()
                    .contentType("application/json")
                    .body("{\"description\":\"Alinhamento\",\"price\":0}")
            .when()
                    .post(WORK_ORDERS_PATH + "/" + workOrderId + "/services")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("deve retornar 422 quando a ordem está cancelada")
        void shouldReturn422WhenWorkOrderLocked() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.CANCELLED);

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body("{\"description\":\"Alinhamento\",\"price\":80.00}")
            .when()
                    .post(WORK_ORDERS_PATH + "/" + workOrderId + "/services")
            .then()
                    .statusCode(422)
                    .extract().response());
            assertEquals("WORK_ORDER_LOCKED", error.code());
        }
    }

    @Nested
    @DisplayName("PATCH /v1/work-orders/{id}/close — close")
    class Close {

        @Test
        @DisplayName("deve usar o valor do orçamento aprovado quando finalValue é omitido")
        void shouldCloseUsingEstimatedValueWhenOmitted() {
            UUID workOrderId = createWorkOrderInProgress(new BigDecimal("150.00"), 2);

            WorkOrderResponseDto closed = given()
                    .contentType("application/json")
                    .body("{}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/close")
            .then()
                    .statusCode(200)
                    .extract().as(WorkOrderResponseDto.class);

            assertEquals(WorkOrderStatus.COMPLETED, closed.status());
            assertEquals(0, new BigDecimal("300.00").compareTo(closed.finalValue()));
        }

        @Test
        @DisplayName("deve usar o finalValue informado quando presente")
        void shouldCloseUsingProvidedFinalValue() {
            UUID workOrderId = createWorkOrderInProgress(new BigDecimal("150.00"), 2);

            WorkOrderResponseDto closed = given()
                    .contentType("application/json")
                    .body("{\"finalValue\":350.00}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/close")
            .then()
                    .statusCode(200)
                    .extract().as(WorkOrderResponseDto.class);

            assertEquals(0, new BigDecimal("350.00").compareTo(closed.finalValue()));
        }

        @Test
        @DisplayName("deve retornar 422 quando a ordem não está IN_PROGRESS")
        void shouldReturn422WhenNotInProgress() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body("{}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/close")
            .then()
                    .statusCode(422)
                    .extract().response());
            assertEquals("INVALID_STATUS_TRANSITION", error.code());
        }

        @Test
        @DisplayName("deve retornar 400 quando finalValue é zero")
        void shouldReturn400WhenFinalValueIsZero() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            given()
                    .contentType("application/json")
                    .body("{\"finalValue\":0}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/close")
            .then()
                    .statusCode(400);
        }
    }

    @Nested
    @DisplayName("Orçamento (peças + serviços), rejeição e estoque")
    class EstimateDecisionAndStock {

        @Test
        @DisplayName("deve somar peças e serviços no total do orçamento")
        void shouldIncludeServicesInEstimateTotal() {
            UUID partId = seedPart(new BigDecimal("50.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            addService(workOrderId, "Mão de obra", new BigDecimal("100.00"));

            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 1);

            assertEquals(0, new BigDecimal("50.00").compareTo(estimate.partsAmount()));
            assertEquals(0, new BigDecimal("100.00").compareTo(estimate.laborAmount()));
            assertEquals(0, new BigDecimal("150.00").compareTo(estimate.totalAmount()));
        }

        @Test
        @DisplayName("deve mover a OS de diagnóstico para aguardando aprovação ao gerar o orçamento")
        void shouldAutoAdvanceToWaitingApprovalOnEstimate() {
            UUID partId = seedPart(new BigDecimal("50.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);

            createEstimate(workOrderId, partId, 1);

            assertEquals(WorkOrderStatus.WAITING_APPROVAL, getWorkOrder(workOrderId).status());
        }

        @Test
        @DisplayName("deve recusar o orçamento e retornar a OS para diagnóstico")
        void shouldRejectEstimateAndReturnToDiagnosis() {
            UUID partId = seedPart(new BigDecimal("50.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 1);

            EstimateResponseDto rejected = rejectEstimate(workOrderId, estimate.estimateId());

            assertEquals(EstimateStatus.REJECTED, rejected.status());
            assertEquals(WorkOrderStatus.DIAGNOSIS, getWorkOrder(workOrderId).status());
        }

        @Test
        @DisplayName("deve dar baixa no estoque ao aprovar e restaurar ao cancelar")
        void shouldDecreaseStockOnApprovalAndRestoreOnCancel() {
            UUID partId = seedPart(new BigDecimal("50.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 3);

            assertEquals(100, stockOf(partId));

            approveEstimate(workOrderId, estimate.estimateId());
            assertEquals(97, stockOf(partId));

            updateStatus(workOrderId, WorkOrderStatus.CANCELLED);
            assertEquals(100, stockOf(partId));
        }

        @Test
        @DisplayName("deve retornar 422 ao aprovar com estoque insuficiente")
        void shouldReturn422WhenInsufficientStock() {
            Part part = new Part("Peça rara", "estoque baixo", new BigDecimal("50.00"), 1, "UN");
            UUID partId = persistInTransaction(() -> partRepository.persist(part)).getId();
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 5);

            given()
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/estimate/" + estimate.estimateId() + "/approve")
            .then()
                    .statusCode(422);

            assertEquals(1, stockOf(partId));
        }
    }

    @Nested
    @DisplayName("Canal público do cliente — /v1/public/work-orders")
    class PublicChannel {

        private static final String PUBLIC_PATH = "/v1/public/work-orders";

        @Test
        @DisplayName("cliente deve acompanhar a OS e aprovar o orçamento")
        void shouldTrackAndApproveAsClient() {
            UUID partId = seedPart(new BigDecimal("80.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 1);

            WorkOrderResponseDto tracked = given()
            .when()
                    .get(PUBLIC_PATH + "/" + workOrderId)
            .then()
                    .statusCode(200)
                    .extract().as(WorkOrderResponseDto.class);
            assertEquals(WorkOrderStatus.WAITING_APPROVAL, tracked.status());

            EstimateResponseDto approved = given()
            .when()
                    .patch(PUBLIC_PATH + "/" + workOrderId + "/estimate/" + estimate.estimateId() + "/approve")
            .then()
                    .statusCode(200)
                    .extract().as(EstimateResponseDto.class);

            assertEquals(EstimateStatus.APPROVED, approved.status());
            assertEquals(WorkOrderStatus.APPROVED, getWorkOrder(workOrderId).status());
        }
    }

    @Nested
    @DisplayName("GET /v1/work-orders/metrics/average-execution-time — tempo médio de execução")
    class Metrics {

        @Test
        @DisplayName("deve retornar o tempo médio de execução após finalizar uma OS")
        void shouldReturnAverageExecutionTime() {
            UUID workOrderId = createWorkOrderInProgress(new BigDecimal("100.00"), 1);
            given()
                    .contentType("application/json")
                    .body("{}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/close")
            .then()
                    .statusCode(200);

            WorkOrderMetricsResponseDto metrics = given()
            .when()
                    .get(WORK_ORDERS_PATH + "/metrics/average-execution-time")
            .then()
                    .statusCode(200)
                    .extract().as(WorkOrderMetricsResponseDto.class);

            assertTrue(metrics.completedWorkOrders() >= 1);
            assertNotNull(metrics.averageExecutionMinutes());
        }
    }
}
