package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.EstimateRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderCloseRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderServiceRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderStatusUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.EstimateItemResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderMetricsResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderServiceResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.AddWorkOrderServiceCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.ChangeWorkOrderStatusCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.CloseWorkOrderCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.CreateEstimateCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.OpenWorkOrderCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.EstimateResult;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderMetricsResult;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderResult;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderServiceResult;

public final class WorkOrderRestMapper {

    private WorkOrderRestMapper() {
    }

    public static OpenWorkOrderCommand toCommand(WorkOrderRequestDto request) {
        return new OpenWorkOrderCommand(
                request.customerId(),
                request.vehicleId(),
                request.description(),
                request.priority(),
                request.assignedWorkerId());
    }

    public static ChangeWorkOrderStatusCommand toCommand(WorkOrderStatusUpdateRequestDto request) {
        return new ChangeWorkOrderStatusCommand(request.status());
    }

    public static CreateEstimateCommand toCommand(EstimateRequestDto request) {
        return new CreateEstimateCommand(request.items().stream()
                .map(item -> new CreateEstimateCommand.Item(
                        item.partId(), item.quantity(), item.unitPrice()))
                .toList());
    }

    public static AddWorkOrderServiceCommand toCommand(WorkOrderServiceRequestDto request) {
        return new AddWorkOrderServiceCommand(
                request.description(), request.price(), request.serviceItemId());
    }

    public static CloseWorkOrderCommand toCommand(WorkOrderCloseRequestDto request) {
        return new CloseWorkOrderCommand(request.finalValue());
    }

    public static PageableResponseDto<WorkOrderResponseDto> toResponse(
            PageableResponseDto<WorkOrderResult> page) {
        return new PageableResponseDto<>(
                page.content().stream().map(WorkOrderRestMapper::toResponse).toList(),
                page.pagination());
    }

    public static WorkOrderResponseDto toResponse(WorkOrderResult result) {
        return new WorkOrderResponseDto(
                result.workOrderId(),
                result.customerId(),
                result.vehicleId(),
                result.description(),
                result.priority(),
                result.status(),
                result.openedAt(),
                result.closedAt(),
                result.estimatedValue(),
                result.finalValue(),
                result.assignedWorkerId());
    }

    public static EstimateResponseDto toResponse(EstimateResult result) {
        return new EstimateResponseDto(
                result.estimateId(),
                result.workOrderId(),
                result.status(),
                result.partsAmount(),
                result.laborAmount(),
                result.totalAmount(),
                result.approvedAt(),
                result.sentAt(),
                result.items().stream()
                        .map(item -> new EstimateItemResponseDto(
                                item.estimateItemId(),
                                item.partId(),
                                item.partName(),
                                item.quantity(),
                                item.unitPrice(),
                                item.totalPrice()))
                        .toList());
    }

    public static WorkOrderServiceResponseDto toResponse(WorkOrderServiceResult result) {
        return new WorkOrderServiceResponseDto(
                result.workOrderServiceId(),
                result.workOrderId(),
                result.description(),
                result.price(),
                result.performedAt(),
                result.serviceItemId());
    }

    public static WorkOrderMetricsResponseDto toResponse(WorkOrderMetricsResult result) {
        return new WorkOrderMetricsResponseDto(
                result.completedWorkOrders(), result.averageExecutionMinutes());
    }
}
