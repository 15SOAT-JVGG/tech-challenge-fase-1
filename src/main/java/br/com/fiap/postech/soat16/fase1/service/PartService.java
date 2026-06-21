package br.com.fiap.postech.soat16.fase1.service;

import static java.lang.Boolean.FALSE;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.ResourceNotFoundException;
import br.com.fiap.postech.soat16.fase1.model.Part;
import br.com.fiap.postech.soat16.fase1.repository.PartRepository;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class PartService {

    public static final String PECA_INSUMO = "Part/Supply";

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @WithSession
    public Uni<List<PartResponseDto>> listAll() {
        return partRepository.listAll()
            .map(parts -> parts.stream().map(PartResponseDto::from).toList());
    }

    @WithSession
    public Uni<PartResponseDto> findById(UUID id) {
        return partRepository.findById(id)
            .onItem().ifNull().failWith(() -> new ResourceNotFoundException(PECA_INSUMO, id))
            .map(PartResponseDto::from);
    }

    @WithSession
    public Uni<List<PartResponseDto>> findLowStock() {
        return partRepository.findLowStock()
            .map(parts -> parts.stream().map(PartResponseDto::from).toList());
    }

    @WithTransaction
    public Uni<PartResponseDto> create(PartRequestDto dto) {
        Part part = new Part(dto.name(), dto.description(), dto.unitPrice(), dto.stockQuantity(), dto.unit(),
            dto.minimumStock(), dto.partType());
        return partRepository.persist(part).map(PartResponseDto::from);
    }

    @WithTransaction
    public Uni<PartResponseDto> update(UUID id, PartRequestDto dto) {
        return partRepository.findById(id)
            .onItem().ifNull().failWith(() -> new ResourceNotFoundException(PECA_INSUMO, id))
            .flatMap(part -> {
                part.update(dto.name(), dto.description(), dto.unitPrice(), dto.stockQuantity(), dto.unit(),
                    dto.minimumStock(), dto.partType());
                return partRepository.persist(part);
            })
            .map(PartResponseDto::from);
    }

    @WithTransaction
    public Uni<PartResponseDto> adjustStock(UUID id, int adjustment) {
        return partRepository.findById(id)
            .onItem().ifNull().failWith(() -> new ResourceNotFoundException(PECA_INSUMO, id))
            .flatMap(part -> {
                if (adjustment > 0) {
                    part.increaseStock(adjustment);
                } else {
                    part.decreaseStock(Math.abs(adjustment));
                }
                return partRepository.persist(part);
            })
            .map(PartResponseDto::from);
    }

    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return partRepository.deleteById(id)
            .flatMap(deleted -> {
                if (FALSE.equals(deleted)) {
                    return Uni.createFrom().failure(new ResourceNotFoundException(PECA_INSUMO, id));
                }
                return Uni.createFrom().voidItem();
            });
    }
}
