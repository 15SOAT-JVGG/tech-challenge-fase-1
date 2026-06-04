package br.com.fiap.postech.soat16.fase1.service;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateLicensePlateException;
import br.com.fiap.postech.soat16.fase1.exception.ResourceNotFoundException;
import br.com.fiap.postech.soat16.fase1.mapper.VehicleMapper;
import br.com.fiap.postech.soat16.fase1.repository.VehicleRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@ApplicationScoped
@AllArgsConstructor
public class VehicleService {

    public static final String VEICULO = "Veículo";

    private VehicleRepository vehicleRepository;
    private VehicleMapper vehicleMapper;

    @WithSession
    public Uni<PageableResponseDto<VehicleResponseDto>> listAll(String q, int page, int size) {
        return Uni.combine().all()
                .unis(vehicleRepository.findPage(page, size), vehicleRepository.count())
                .asTuple().map(tuple -> {
                    var content = tuple.getItem1().stream().map(vehicleMapper::toResponse).toList();
                    var totalElements = tuple.getItem2();
                    return PageableResponseDto.of(content, page, size, totalElements);
                });
    }

    @WithSession
    public Uni<VehicleResponseDto> findById(Long id) {
        return vehicleRepository.findById(id)
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(VEICULO, id))
                .map(vehicleMapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> create(VehicleRequestDto dto) {
        return vehicleRepository.existsByLicensePlate(dto.licensePlate())
                .flatMap(exists -> {
                    if (TRUE.equals(exists)) {
                        throw new DuplicateLicensePlateException();
                    }
                    return vehicleRepository.persist(vehicleMapper.toEntity(dto, null))
                            .replaceWithVoid();
                });
    }

    @WithTransaction
    public Uni<VehicleResponseDto> update(Long id, VehicleRequestDto request) {
        return vehicleRepository.findById(id)
                .flatMap(entity -> {
                    if (entity == null) {
                        throw new ResourceNotFoundException(VEICULO, id);
                    }
                    vehicleMapper.updateEntity(entity, request);
                    return vehicleRepository.persist(entity)
                            .map(vehicleMapper::toResponse);
                });
    }

    @WithTransaction
    public Uni<Void> delete(Long id) {
        return vehicleRepository.deleteById(id)
                .flatMap(deleted -> {
                    if (FALSE.equals(deleted)) {
                        throw new ResourceNotFoundException(VEICULO, id);
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}