package br.com.fiap.postech.soat16.fase1.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.ServiceItem;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

@ApplicationScoped
public class ServiceItemRepository implements PanacheRepositoryBase<ServiceItem, UUID> {
}
