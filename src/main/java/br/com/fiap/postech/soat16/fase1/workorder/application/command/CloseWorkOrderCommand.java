package br.com.fiap.postech.soat16.fase1.workorder.application.command;

import java.math.BigDecimal;

public record CloseWorkOrderCommand(BigDecimal finalValue) { }
