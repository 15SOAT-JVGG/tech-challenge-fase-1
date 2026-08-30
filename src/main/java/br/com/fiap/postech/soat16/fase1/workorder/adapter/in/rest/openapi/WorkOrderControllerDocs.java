package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.openapi;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.EstimateRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderCloseRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderServiceRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderStatusUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.OpenedWorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderMetricsResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderServiceResponseDto;

import io.smallrye.mutiny.Uni;

@Path("/v1/work-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Work Orders", description = "Work order lifecycle: estimates, services and status management")
public interface WorkOrderControllerDocs {

    @GET
    @Operation(summary = "List the operational queue",
            description = "Returns a paginated list of the work orders still in progress. COMPLETED and DELIVERED "
                    + "work orders are excluded. Results are grouped by IN_PROGRESS, WAITING_APPROVAL, DIAGNOSIS and "
                    + "RECEIVED, and within each group the oldest opening date comes first.")
    @APIResponse(responseCode = "200", description = "Work orders retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PageableResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid query parameters")
    Uni<PageableResponseDto<WorkOrderResponseDto>> findOperationalQueue(@BeanParam @Valid PageableRequestDto pageable);

    @GET
    @Path("/metrics/average-execution-time")
    @Operation(summary = "Average execution time",
            description = "Returns the average execution time (between opening and completion) across all "
                    + "closed work orders, in minutes, plus the sample size.")
    @APIResponse(responseCode = "200", description = "Metric computed successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = WorkOrderMetricsResponseDto.class)))
    Uni<WorkOrderMetricsResponseDto> averageExecutionTime();

    @GET
    @Path("/{id}")
    @Operation(summary = "Get work order by ID", description = "Returns a single work order by identifier.")
    @APIResponse(responseCode = "200", description = "Work order found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = WorkOrderResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Work order not found")
    Uni<WorkOrderResponseDto> findById(
            @Parameter(name = "id", description = "Work order identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);

    @POST
    @Operation(summary = "Open work order",
            description = "Creates a new work order for an existing customer and vehicle. Initial status is "
                    + "RECEIVED, default priority is MEDIUM, openedAt is set automatically. When the initial "
                    + "request carries catalog services or parts, the matching pending estimate is created in "
                    + "the same transaction, with prices snapshotted from the catalog.")
    @APIResponse(responseCode = "201", description = "Work order opened successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OpenedWorkOrderResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "404", description = "Customer, vehicle, worker, service item or part not found")
    Uni<Response> create(
            @RequestBody(description = "Work order data for opening, including the initial service and part request")
            @Valid WorkOrderRequestDto body);

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Update work order status",
            description = "Moves the work order to the next status in the lifecycle. Use /close to reach "
                    + "COMPLETED. Moving to IN_PROGRESS requires an approved estimate. DELIVERED blocks "
                    + "further changes.")
    @APIResponse(responseCode = "200", description = "Status updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = WorkOrderResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Work order not found")
    @APIResponse(responseCode = "422", description = "Invalid status transition or missing approved estimate")
    Uni<WorkOrderResponseDto> updateStatus(
            @Parameter(name = "id", description = "Work order identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @RequestBody(description = "Target status")
            @Valid WorkOrderStatusUpdateRequestDto body);

    @POST
    @Path("/{id}/estimate")
    @Operation(summary = "Create estimate", description = "Creates a new estimate with items priced from the "
            + "parts catalog. totalAmount is the sum of (quantity * unitPrice) for every item.")
    @APIResponse(responseCode = "201", description = "Estimate created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EstimateResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "404", description = "Work order or part not found")
    @APIResponse(responseCode = "422",
            description = "Ordem entregue ou concluída por recusa do orçamento")
    Uni<Response> createEstimate(
            @Parameter(name = "id", description = "Work order identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @RequestBody(description = "Estimate items")
            @Valid EstimateRequestDto body);

    @PATCH
    @Path("/{id}/estimate/{estimateId}/approve")
    @Operation(summary = "Approve estimate",
            description = "Approves a pending estimate. Sets the work order's estimatedValue to the estimate's "
                    + "totalAmount and, if the work order is WAITING_APPROVAL, advances it to IN_PROGRESS.")
    @APIResponse(responseCode = "200", description = "Estimate approved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EstimateResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Work order or estimate not found")
    @APIResponse(responseCode = "409", description = "Estimate already approved or rejected")
    Uni<EstimateResponseDto> approveEstimate(
            @Parameter(name = "id", description = "Work order identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @Parameter(name = "estimateId", description = "Estimate identifier", required = true,
                    in = ParameterIn.PATH)
            @PathParam("estimateId") UUID estimateId);

    @PATCH
    @Path("/{id}/estimate/{estimateId}/reject")
    @Operation(summary = "Reject estimate",
            description = "Rejects a pending estimate. If the work order is WAITING_APPROVAL it is completed and "
                    + "records cancelledAt. No stock is reserved on rejection.")
    @APIResponse(responseCode = "200", description = "Estimate rejected successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EstimateResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Work order or estimate not found")
    @APIResponse(responseCode = "409", description = "Estimate already approved or rejected")
    Uni<EstimateResponseDto> rejectEstimate(
            @Parameter(name = "id", description = "Work order identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @Parameter(name = "estimateId", description = "Estimate identifier", required = true,
                    in = ParameterIn.PATH)
            @PathParam("estimateId") UUID estimateId);

    @POST
    @Path("/{id}/services")
    @Operation(summary = "Add service", description = "Adds a labor service line performed on the work order.")
    @APIResponse(responseCode = "201", description = "Service added successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = WorkOrderServiceResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "404", description = "Work order not found")
    @APIResponse(responseCode = "422",
            description = "Ordem entregue ou concluída por recusa do orçamento")
    Uni<Response> addService(
            @Parameter(name = "id", description = "Work order identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @RequestBody(description = "Service data")
            @Valid WorkOrderServiceRequestDto body);

    @PATCH
    @Path("/{id}/close")
    @Operation(summary = "Close work order",
            description = "Finalizes a work order that is IN_PROGRESS and has an approved estimate. Sets "
                    + "closedAt and finalValue (defaults to estimatedValue when omitted), and moves status to "
                    + "COMPLETED.")
    @APIResponse(responseCode = "200", description = "Work order closed successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = WorkOrderResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Work order not found")
    @APIResponse(responseCode = "422", description = "Work order is not in progress or has no approved estimate")
    Uni<WorkOrderResponseDto> close(
            @Parameter(name = "id", description = "Work order identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @RequestBody(description = "Optional final value override")
            @Valid WorkOrderCloseRequestDto body);
}
