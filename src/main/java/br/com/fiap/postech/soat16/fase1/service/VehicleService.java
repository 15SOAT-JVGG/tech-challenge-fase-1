package br.com.fiap.postech.soat16.fase1.service;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleDto;
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

    private VehicleRepository vehicleRepository;
    private VehicleMapper vehicleMapper;

    @WithSession
    public Uni<PageableResponseDto<VehicleResponseDto>> listAll(PageableRequestDto pageable, VehicleFilterDto filter) {
        return Uni.combine().all()
                .unis(vehicleRepository.findPageWithFilter(pageable, filter), vehicleRepository.countWithFilter(filter))
                .asTuple().map(tuple -> {
                    var content = tuple.getItem1().stream().map(vehicleMapper::toResponse).toList();
                    var totalElements = tuple.getItem2();
                    return PageableResponseDto.of(content, pageable.getPage(), pageable.getSize(), totalElements);
                });
    }

    @WithSession
    public Uni<VehicleResponseDto> findById(Long id) {
        return vehicleRepository.findById(id)
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException("Vehicle not found"))
                .map(vehicleMapper::toResponse);
    }

    @WithSession
    public Uni<VehicleResponseDto> findByLicensePlate(String licensePlate) {
        return vehicleRepository.findByLicensePlate(licensePlate)
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException("Vehicle not found"))
                .map(vehicleMapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> create(VehicleDto dto) {
        return vehicleRepository.existsByLicensePlate(dto.licensePlate())
                .flatMap(exists -> {
                    if (TRUE.equals(exists)) {
                        return Uni.createFrom().failure(new DuplicateLicensePlateException());
                    }
                    return vehicleRepository.persist(vehicleMapper.toEntity(dto, null))
                            .replaceWithVoid();
                });
    }

    @WithTransaction
    public Uni<VehicleResponseDto> update(Long id, VehicleDto request) {
        return vehicleRepository.findById(id)
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException("Vehicle not found"))
                .flatMap(entity -> {
                    vehicleMapper.updateEntity(entity, request);
                    return vehicleRepository.persist(entity).map(vehicleMapper::toResponse);
                });
    }

    @WithTransaction
    public Uni<Void> delete(Long id) {
        return vehicleRepository.deleteById(id)
                .flatMap(deleted -> {
                    if (FALSE.equals(deleted)) {
                        return Uni.createFrom().failure(new ResourceNotFoundException("Vehicle not found"));
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}