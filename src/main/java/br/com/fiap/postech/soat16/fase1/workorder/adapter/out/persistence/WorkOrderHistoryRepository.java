package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderHistory;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;

@ApplicationScoped
public class WorkOrderHistoryRepository implements PanacheRepository<WorkOrderHistory> {
}
