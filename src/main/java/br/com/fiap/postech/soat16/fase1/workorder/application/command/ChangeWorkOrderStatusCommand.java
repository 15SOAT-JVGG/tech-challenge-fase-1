package br.com.fiap.postech.soat16.fase1.workorder.application.command;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

public record ChangeWorkOrderStatusCommand(WorkOrderStatus status) { }
