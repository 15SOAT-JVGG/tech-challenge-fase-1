package br.com.fiap.postech.soat16.fase1.workorder.application.result;

public record WorkOrderMetricsResult(
        long completedWorkOrders,
        Double averageExecutionMinutes
) { }
