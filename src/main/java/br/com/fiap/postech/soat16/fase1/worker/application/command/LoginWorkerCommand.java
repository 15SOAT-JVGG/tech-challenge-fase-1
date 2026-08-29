package br.com.fiap.postech.soat16.fase1.worker.application.command;

public record LoginWorkerCommand(
        String email,
        String password
) { }
