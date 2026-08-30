package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.controller;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

import br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.CustomerRepository;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.enums.DocumentType;
import br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.PartRepository;
import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.out.persistence.ServiceItemRepository;
import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.dto.ApiErrorResponseDto;
import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.JwtKeyPairTestResource;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.VehicleRepository;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.OpenedWorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderMetricsResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderServiceResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.WorkOrderRepository;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
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

    @Inject
    ServiceItemRepository serviceItemRepository;

    @Inject
    WorkOrderRepository workOrderRepository;

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
        RestAssured.replaceFiltersWith(List.of());
    }

    // Persiste fora do event loop, estabelecendo o contexto Vert.x exigido pelo Hibernate
    // Reactive, como no DataSeeder e no AuthControllerIT.
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
        return persistInTransaction(() -> customerRepository.save(customer)).getId();
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
        return persistInTransaction(() -> vehicleRepository.save(vehicle)).getId();
    }

    private UUID seedPart(BigDecimal unitPrice) {
        Part part = new Part("Filtro de óleo", "Filtro de óleo padrão", unitPrice, 100, "UN");
        return persistInTransaction(() -> partRepository.save(part)).getId();
    }

    private UUID seedServiceItem(BigDecimal basePrice) {
        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setName("Alinhamento e balanceamento");
        serviceItem.setDescription("Serviço padrão de suspensão");
        serviceItem.setBasePrice(basePrice);
        serviceItem.setEstimatedDurationMinutes(60);
        serviceItem.setActive(true);
        return persistInTransaction(() -> serviceItemRepository.save(serviceItem)).getId();
    }

    private OpenedWorkOrderResponseDto openWorkOrder(String body) {
        return given()
                .contentType("application/json")
                .body(body)
        .when()
                .post(WORK_ORDERS_PATH)
        .then()
                .statusCode(201)
                .extract().as(OpenedWorkOrderResponseDto.class);
    }

    private UUID createWorkOrder(UUID customerId, UUID vehicleId) {
        String body = """
                {"customerId":"%s","vehicleId":"%s","description":"Revisão geral"}
                """.formatted(customerId, vehicleId);

        return openWorkOrder(body).workOrder().workOrderId();
    }

    private PageableResponseDto<WorkOrderResponseDto> operationalQueuePage(int page, int size) {
        return given()
                .queryParam("page", page)
                .queryParam("size", size)
        .when()
                .get(WORK_ORDERS_PATH)
        .then()
                .statusCode(200)
                .extract().as(new TypeRef<>() {
                });
    }

    // A base é compartilhada por toda a classe, então a fila é percorrida por inteiro antes de
    // afirmar sobre a posição relativa das ordens criadas pelo teste.
    private List<WorkOrderResponseDto> operationalQueue() {
        List<WorkOrderResponseDto> queue = new ArrayList<>();
        PageableResponseDto<WorkOrderResponseDto> firstPage = operationalQueuePage(0, 100);
        for (int index = 0; index < firstPage.pagination().totalPages(); index++) {
            PageableResponseDto<WorkOrderResponseDto> current =
                    index == 0 ? firstPage : operationalQueuePage(index, 100);
            queue.addAll(current.content());
        }
        return queue;
    }

    private List<UUID> operationalQueueIds() {
        return operationalQueue().stream().map(WorkOrderResponseDto::workOrderId).toList();
    }

    private boolean workOrderExistsForVehicle(UUID vehicleId) {
        return operationalQueue().stream().anyMatch(workOrder -> vehicleId.equals(workOrder.vehicleId()));
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

    private void repricePart(UUID partId, BigDecimal unitPrice) {
        persistInTransaction(() -> partRepository.find("id = ?1", partId).firstResult()
                .invoke(entity -> entity.setUnitPrice(unitPrice)));
    }

    // A API não permite escolher a data de abertura, então a fila só pode ser exercitada por data
    // retroagindo a OS direto na base.
    private void backdateOpening(UUID workOrderId, Duration age) {
        persistInTransaction(() -> workOrderRepository.find("id = ?1", workOrderId).firstResult()
                .invoke(entity -> entity.setOpenedAt(entity.getOpenedAt().minus(age))));
    }

    private void repriceServiceItem(UUID serviceItemId, BigDecimal basePrice) {
        persistInTransaction(() -> serviceItemRepository.find("id = ?1", serviceItemId).firstResult()
                .invoke(entity -> entity.setBasePrice(basePrice)));
    }

    private UUID createWorkOrderInProgress(BigDecimal unitPrice, int quantity) {
        UUID customerId = seedCustomer();
        UUID vehicleId = seedVehicle();
        UUID partId = seedPart(unitPrice);
        UUID workOrderId = createWorkOrder(customerId, vehicleId);

        updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
        updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
        EstimateResponseDto estimate = createEstimate(workOrderId, partId, quantity);
        approveEstimate(workOrderId, estimate.estimateId());

        return workOrderId;
    }

    private ApiErrorResponseDto extractError(Response response) {
        return response.as(ApiErrorResponseDto.class);
    }

    @Nested
    @DisplayName("Fluxo completo da ordem de serviço")
    class FullLifecycle {

        @Test
        @DisplayName("deve percorrer RECEIVED -> DIAGNOSIS -> WAITING_APPROVAL -> IN_PROGRESS -> COMPLETED -> DELIVERED")
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
            assertEquals(WorkOrderStatus.IN_PROGRESS, afterApproval.status());
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
        @DisplayName("deve rejeitar o status CANCELLED removido do contrato")
        void shouldRejectRemovedCancelledStatus() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            given()
                    .contentType("application/json")
                    .body("{\"status\":\"CANCELLED\"}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/status")
            .then()
                    .statusCode(400);
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
                    .body("{\"status\":\"DIAGNOSIS\"}")
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
        @DisplayName("deve devolver a identificação da ordem recém-aberta no corpo e no Location")
        void shouldReturnCreatedWorkOrderIdentification() {
            UUID vehicleId = seedVehicle();
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":"Revisão geral"}
                    """.formatted(seedCustomer(), vehicleId);

            Response response = given()
                    .contentType("application/json")
                    .body(body)
            .when()
                    .post(WORK_ORDERS_PATH)
            .then()
                    .statusCode(201)
                    .extract().response();

            OpenedWorkOrderResponseDto opened = response.as(OpenedWorkOrderResponseDto.class);
            UUID workOrderId = opened.workOrder().workOrderId();

            assertNotNull(workOrderId);
            assertNull(opened.estimate());
            assertEquals(vehicleId, opened.workOrder().vehicleId());
            assertTrue(response.header("Location").endsWith(WORK_ORDERS_PATH + "/" + workOrderId));
            assertEquals(workOrderId, getWorkOrder(workOrderId).workOrderId());
        }

        @Test
        @DisplayName("deve abrir a ordem e o orçamento pendente da solicitação inicial em uma única chamada")
        void shouldOpenWorkOrderWithInitialPendingEstimate() {
            UUID partId = seedPart(new BigDecimal("50.00"));
            UUID serviceItemId = seedServiceItem(new BigDecimal("120.00"));
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":"Revisão dos 10.000km",
                     "services":[{"serviceItemId":"%s"}],
                     "parts":[{"partId":"%s","quantity":3}]}
                    """.formatted(seedCustomer(), seedVehicle(), serviceItemId, partId);

            OpenedWorkOrderResponseDto opened = openWorkOrder(body);

            assertEquals(WorkOrderStatus.RECEIVED, opened.workOrder().status());
            EstimateResponseDto estimate = opened.estimate();
            assertNotNull(estimate);
            assertEquals(EstimateStatus.PENDING, estimate.status());
            assertEquals(opened.workOrder().workOrderId(), estimate.workOrderId());
            assertEquals(0, new BigDecimal("150.00").compareTo(estimate.partsAmount()));
            assertEquals(0, new BigDecimal("120.00").compareTo(estimate.laborAmount()));
            assertEquals(0, new BigDecimal("270.00").compareTo(estimate.totalAmount()));
            assertEquals(1, estimate.items().size());
            assertEquals(0, new BigDecimal("50.00").compareTo(estimate.items().get(0).unitPrice()));

            WorkOrderResponseDto persisted = getWorkOrder(opened.workOrder().workOrderId());
            assertEquals(0, new BigDecimal("270.00").compareTo(persisted.estimatedValue()));
        }

        @Test
        @DisplayName("deve manter os preços do orçamento inalterados quando o catálogo é atualizado depois")
        void shouldKeepEstimatePricesAfterCatalogUpdate() {
            UUID partId = seedPart(new BigDecimal("50.00"));
            UUID serviceItemId = seedServiceItem(new BigDecimal("120.00"));
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":"Revisão",
                     "services":[{"serviceItemId":"%s"}],
                     "parts":[{"partId":"%s","quantity":1}]}
                    """.formatted(seedCustomer(), seedVehicle(), serviceItemId, partId);

            OpenedWorkOrderResponseDto opened = openWorkOrder(body);
            UUID workOrderId = opened.workOrder().workOrderId();

            repricePart(partId, new BigDecimal("999.00"));
            repriceServiceItem(serviceItemId, new BigDecimal("999.00"));

            assertEquals(0, new BigDecimal("170.00").compareTo(getWorkOrder(workOrderId).estimatedValue()));

            // A aprovação relê o orçamento gravado, expondo o preço unitário efetivamente persistido.
            EstimateResponseDto persisted = approveEstimate(workOrderId, opened.estimate().estimateId());
            assertEquals(0, new BigDecimal("50.00").compareTo(persisted.items().get(0).unitPrice()));
            assertEquals(0, new BigDecimal("120.00").compareTo(persisted.laborAmount()));
            assertEquals(0, new BigDecimal("170.00").compareTo(persisted.totalAmount()));
        }

        @Test
        @DisplayName("deve reverter as linhas de serviço já gravadas quando uma peça seguinte é inválida")
        void shouldRollbackPersistedServicesWhenAPartIsInvalid() {
            UUID vehicleId = seedVehicle();
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":"Revisão",
                     "services":[{"serviceItemId":"%s"}],
                     "parts":[{"partId":"%s","quantity":1}]}
                    """.formatted(seedCustomer(), vehicleId,
                    seedServiceItem(new BigDecimal("120.00")), UUID.randomUUID());

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body(body)
            .when()
                    .post(WORK_ORDERS_PATH)
            .then()
                    .statusCode(404)
                    .extract().response());

            assertEquals("ESTIMATE_PART_NOT_FOUND", error.code());
            assertFalse(workOrderExistsForVehicle(vehicleId));
        }

        @Test
        @DisplayName("deve gravar todos os serviços quando a solicitação inicial traz vários itens de catálogo")
        void shouldOpenWorkOrderWithSeveralRequestedServices() {
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":"Revisão",
                     "services":[{"serviceItemId":"%s"},{"serviceItemId":"%s"},{"serviceItemId":"%s"}]}
                    """.formatted(seedCustomer(), seedVehicle(),
                    seedServiceItem(new BigDecimal("120.00")),
                    seedServiceItem(new BigDecimal("80.50")),
                    seedServiceItem(new BigDecimal("40.00")));

            OpenedWorkOrderResponseDto opened = openWorkOrder(body);

            assertEquals(0, new BigDecimal("240.50").compareTo(opened.estimate().laborAmount()));
            assertEquals(0, BigDecimal.ZERO.compareTo(opened.estimate().partsAmount()));
            assertEquals(0, new BigDecimal("240.50").compareTo(opened.estimate().totalAmount()));
        }

        @Test
        @DisplayName("deve retornar 404 e não abrir a ordem quando o item de serviço não existe")
        void shouldReturn404WhenServiceItemNotFound() {
            UUID vehicleId = seedVehicle();
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":"Revisão",
                     "services":[{"serviceItemId":"%s"}]}
                    """.formatted(seedCustomer(), vehicleId, UUID.randomUUID());

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body(body)
            .when()
                    .post(WORK_ORDERS_PATH)
            .then()
                    .statusCode(404)
                    .extract().response());

            assertEquals("SERVICE_ITEM_NOT_FOUND", error.code());
            assertFalse(workOrderExistsForVehicle(vehicleId));
        }

        @Test
        @DisplayName("deve retornar 404 e não abrir a ordem quando a peça não existe")
        void shouldReturn404WhenRequestedPartNotFound() {
            UUID vehicleId = seedVehicle();
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":"Revisão",
                     "parts":[{"partId":"%s","quantity":1}]}
                    """.formatted(seedCustomer(), vehicleId, UUID.randomUUID());

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body(body)
            .when()
                    .post(WORK_ORDERS_PATH)
            .then()
                    .statusCode(404)
                    .extract().response());

            assertEquals("ESTIMATE_PART_NOT_FOUND", error.code());
            assertFalse(workOrderExistsForVehicle(vehicleId));
        }

        @Test
        @DisplayName("deve retornar 400 quando a quantidade de uma peça solicitada é menor que 1")
        void shouldReturn400WhenRequestedQuantityIsBelowOne() {
            UUID vehicleId = seedVehicle();
            String body = """
                    {"customerId":"%s","vehicleId":"%s","description":"Revisão",
                     "parts":[{"partId":"%s","quantity":0}]}
                    """.formatted(seedCustomer(), vehicleId, seedPart(new BigDecimal("50.00")));

            given()
                    .contentType("application/json")
                    .body(body)
            .when()
                    .post(WORK_ORDERS_PATH)
            .then()
                    .statusCode(400);

            assertFalse(workOrderExistsForVehicle(vehicleId));
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
        @DisplayName("deve rejeitar o status APPROVED removido do contrato")
        void shouldRejectRemovedApprovedStatus() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            given()
                    .contentType("application/json")
                    .body("{\"status\":\"APPROVED\"}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/status")
            .then()
                    .statusCode(400)
                    .extract().response();
        }

        @Test
        @DisplayName("deve rejeitar pular de RECEIVED para IN_PROGRESS")
        void shouldRejectSkippingCanonicalStages() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());

            ApiErrorResponseDto error = extractError(given()
                    .contentType("application/json")
                    .body("{\"status\":\"IN_PROGRESS\"}")
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
                    .body("{\"status\":\"IN_PROGRESS\"}")
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

    }

    @Nested
    @DisplayName("PATCH /v1/work-orders/{id}/estimate/{estimateId}/approve — approveEstimate")
    class ApproveEstimate {

        @Test
        @DisplayName("deve aprovar o orçamento e avançar a ordem para IN_PROGRESS")
        void shouldApproveAndAdvanceStatus() {
            UUID partId = seedPart(new BigDecimal("80.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 1);

            EstimateResponseDto approved = approveEstimate(workOrderId, estimate.estimateId());

            assertEquals(EstimateStatus.APPROVED, approved.status());
            assertNotNull(approved.approvedAt());
            assertEquals(WorkOrderStatus.IN_PROGRESS, getWorkOrder(workOrderId).status());
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
        @DisplayName("deve recusar o orçamento, concluir a OS e registrar o cancelamento")
        void shouldRejectEstimateAndCompleteWorkOrder() {
            UUID partId = seedPart(new BigDecimal("50.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            updateStatus(workOrderId, WorkOrderStatus.WAITING_APPROVAL);
            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 1);

            EstimateResponseDto rejected = rejectEstimate(workOrderId, estimate.estimateId());

            assertEquals(EstimateStatus.REJECTED, rejected.status());
            WorkOrderResponseDto workOrder = getWorkOrder(workOrderId);
            assertEquals(WorkOrderStatus.COMPLETED, workOrder.status());
            assertNotNull(workOrder.closedAt());
            assertNotNull(workOrder.cancelledAt());
        }

        @Test
        @DisplayName("deve bloquear alterações e entrega depois da recusa do orçamento")
        void shouldLockWorkOrderAfterEstimateRejection() {
            UUID partId = seedPart(new BigDecimal("50.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 1);
            rejectEstimate(workOrderId, estimate.estimateId());

            ApiErrorResponseDto serviceError = extractError(given()
                    .contentType("application/json")
                    .body("{\"description\":\"Tentativa bloqueada\",\"price\":10.00}")
            .when()
                    .post(WORK_ORDERS_PATH + "/" + workOrderId + "/services")
            .then()
                    .statusCode(422)
                    .extract().response());

            ApiErrorResponseDto deliveryError = extractError(given()
                    .contentType("application/json")
                    .body("{\"status\":\"DELIVERED\"}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/status")
            .then()
                    .statusCode(422)
                    .extract().response());

            assertEquals("WORK_ORDER_LOCKED", serviceError.code());
            assertEquals("WORK_ORDER_LOCKED", deliveryError.code());
        }

        @Test
        @DisplayName("deve retornar 422 ao aprovar com estoque insuficiente")
        void shouldReturn422WhenInsufficientStock() {
            Part part = new Part("Peça rara", "estoque baixo", new BigDecimal("50.00"), 1, "UN");
            UUID partId = persistInTransaction(() -> partRepository.save(part)).getId();
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
            assertEquals(WorkOrderStatus.IN_PROGRESS, getWorkOrder(workOrderId).status());
        }

        @Test
        @DisplayName("cliente deve recusar o orçamento e concluir a OS com cancelamento")
        void shouldRejectAsClient() {
            UUID partId = seedPart(new BigDecimal("80.00"));
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            EstimateResponseDto estimate = createEstimate(workOrderId, partId, 1);

            EstimateResponseDto rejected = given()
            .when()
                    .patch(PUBLIC_PATH + "/" + workOrderId + "/estimate/" + estimate.estimateId() + "/reject")
            .then()
                    .statusCode(200)
                    .extract().as(EstimateResponseDto.class);

            WorkOrderResponseDto workOrder = getWorkOrder(workOrderId);
            assertEquals(EstimateStatus.REJECTED, rejected.status());
            assertEquals(WorkOrderStatus.COMPLETED, workOrder.status());
            assertNotNull(workOrder.cancelledAt());
        }
    }

    @Nested
    @DisplayName("GET /v1/work-orders — fila operacional")
    class OperationalQueue {

        @Test
        @DisplayName("deve agrupar por estágio de trabalho, do IN_PROGRESS ao RECEIVED")
        void shouldGroupByWorkStage() {
            UUID received = createWorkOrder(seedCustomer(), seedVehicle());
            UUID diagnosis = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(diagnosis, WorkOrderStatus.DIAGNOSIS);
            UUID waitingApproval = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(waitingApproval, WorkOrderStatus.DIAGNOSIS);
            updateStatus(waitingApproval, WorkOrderStatus.WAITING_APPROVAL);
            UUID inProgress = createWorkOrderInProgress(new BigDecimal("100.00"), 1);

            List<UUID> queue = operationalQueueIds();

            assertTrue(queue.containsAll(List.of(inProgress, waitingApproval, diagnosis, received)));
            assertTrue(queue.indexOf(inProgress) < queue.indexOf(waitingApproval));
            assertTrue(queue.indexOf(waitingApproval) < queue.indexOf(diagnosis));
            assertTrue(queue.indexOf(diagnosis) < queue.indexOf(received));
        }

        // A OS aberta por último é retroagida para que a ordem de criação e a ordem esperada fiquem
        // invertidas — sem isso o teste passaria mesmo se a fila devolvesse na ordem de inserção.
        @Test
        @DisplayName("deve colocar a OS mais antiga à frente dentro do mesmo status")
        void shouldPlaceTheOldestFirstWithinTheSameStatus() {
            UUID createdFirst = createWorkOrder(seedCustomer(), seedVehicle());
            UUID oldest = createWorkOrder(seedCustomer(), seedVehicle());
            backdateOpening(oldest, Duration.ofDays(1));

            List<UUID> queue = operationalQueueIds();

            assertTrue(queue.containsAll(List.of(createdFirst, oldest)));
            assertTrue(queue.indexOf(oldest) < queue.indexOf(createdFirst));
        }

        @Test
        @DisplayName("deve excluir da fila as OS concluídas, entregues e concluídas por recusa")
        void shouldExcludeClosedWorkOrders() {
            UUID completed = createWorkOrderInProgress(new BigDecimal("100.00"), 1);
            closeWorkOrder(completed);
            UUID delivered = createWorkOrderInProgress(new BigDecimal("100.00"), 1);
            closeWorkOrder(delivered);
            updateStatus(delivered, WorkOrderStatus.DELIVERED);
            UUID cancelled = createWorkOrderRejectedByCustomer();

            List<UUID> queue = operationalQueueIds();

            assertFalse(queue.contains(completed));
            assertFalse(queue.contains(delivered));
            assertFalse(queue.contains(cancelled));
            assertEquals(WorkOrderStatus.COMPLETED, getWorkOrder(cancelled).status());
            assertNotNull(getWorkOrder(cancelled).cancelledAt());
        }

        @Test
        @DisplayName("deve contar apenas as OS que continuam na fila")
        void shouldCountOnlyTheQueuedWorkOrders() {
            long before = operationalQueuePage(0, 1).pagination().totalElements();

            createWorkOrder(seedCustomer(), seedVehicle());
            closeWorkOrder(createWorkOrderInProgress(new BigDecimal("100.00"), 1));

            assertEquals(before + 1, operationalQueuePage(0, 1).pagination().totalElements());
        }

        @Test
        @DisplayName("deve paginar exatamente o conjunto filtrado")
        void shouldPaginateTheFilteredSet() {
            createWorkOrder(seedCustomer(), seedVehicle());
            createWorkOrder(seedCustomer(), seedVehicle());

            PageableResponseDto<WorkOrderResponseDto> firstPage = operationalQueuePage(0, 1);
            PageableResponseDto<WorkOrderResponseDto> secondPage = operationalQueuePage(1, 1);

            assertEquals(1, firstPage.content().size());
            assertEquals(operationalQueueIds().size(), firstPage.pagination().totalElements());
            assertEquals(firstPage.pagination().totalElements(), secondPage.pagination().totalElements());
            assertTrue(firstPage.pagination().hasNext());
            assertTrue(secondPage.pagination().hasPrevious());
            assertNotEquals(firstPage.content().get(0).workOrderId(), secondPage.content().get(0).workOrderId());
        }

        @Test
        @DisplayName("deve devolver uma página vazia além da última")
        void shouldReturnAnEmptyPageBeyondTheLastOne() {
            createWorkOrder(seedCustomer(), seedVehicle());

            PageableResponseDto<WorkOrderResponseDto> page =
                    operationalQueuePage(operationalQueuePage(0, 100).pagination().totalPages(), 100);

            assertTrue(page.content().isEmpty());
            assertFalse(page.pagination().hasNext());
        }

        private void closeWorkOrder(UUID workOrderId) {
            given()
                    .contentType("application/json")
                    .body("{}")
            .when()
                    .patch(WORK_ORDERS_PATH + "/" + workOrderId + "/close")
            .then()
                    .statusCode(200);
        }

        private UUID createWorkOrderRejectedByCustomer() {
            UUID workOrderId = createWorkOrder(seedCustomer(), seedVehicle());
            updateStatus(workOrderId, WorkOrderStatus.DIAGNOSIS);
            EstimateResponseDto estimate = createEstimate(workOrderId, seedPart(new BigDecimal("60.00")), 1);
            rejectEstimate(workOrderId, estimate.estimateId());
            return workOrderId;
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
