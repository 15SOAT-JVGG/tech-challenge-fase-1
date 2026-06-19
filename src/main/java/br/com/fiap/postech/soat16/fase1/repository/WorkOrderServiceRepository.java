package br.com.fiap.postech.soat16.fase1.repository;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.WorkOrderService;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;

@ApplicationScoped
public class WorkOrderServiceRepository implements PanacheRepository<WorkOrderService> {
}
