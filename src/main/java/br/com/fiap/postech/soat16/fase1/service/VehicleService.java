package br.com.fiap.postech.soat16.fase1.service;

import static java.lang.Boolean.TRUE;

import br.com.fiap.postech.soat16.fase1.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.VehicleNotFoundException;
import br.com.fiap.postech.soat16.fase1.repository.CustomerRepository;
import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateLicensePlateException;
import br.com.fiap.postech.soat16.fase1.mapper.VehicleMapper;
import br.com.fiap.postech.soat16.fase1.repository.VehicleRepository;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.AllArgsConstructor;

import java.util.UUID;

@ApplicationScoped
@AllArgsConstructor
public class VehicleService {

    private CustomerRepository customerRepository;
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
    public Uni<VehicleResponseDto> findById(UUID id) {
        return vehicleRepository.findByVehicleId(id)
                .onItem().ifNull().failWith(VehicleNotFoundException::new)
                .map(vehicleMapper::toResponse);
    }

    @WithSession
    public Uni<VehicleResponseDto> findByLicensePlate(String licensePlate) {
        return vehicleRepository.findByLicensePlate(licensePlate)
                .onItem().ifNull().failWith(() -> new VehicleNotFoundException(licensePlate))
                .map(vehicleMapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> create(VehicleDto dto) {
        return customerRepository.findByCustomerId(dto.customerId())
                .onItem().ifNull().failWith(CustomerNotFoundException::new)
                .flatMap(customer -> vehicleRepository.existsByLicensePlate(dto.licensePlate())
                        .flatMap(exists -> {
                            if (TRUE.equals(exists)) {
                                return Uni.createFrom().failure(new DuplicateLicensePlateException());
                            }
                            var vehicle = vehicleMapper.toEntity(dto, customer);
                            if (vehicle == null) {
                                return Uni.createFrom().failure(new IllegalArgumentException("Invalid vehicle data"));
                            }
                            return vehicleRepository.persist(vehicle).replaceWithVoid();
                        }));
    }

    @WithTransaction
    public Uni<VehicleResponseDto> update(UUID id, VehicleDto request) {
        return vehicleRepository.findByVehicleId(id)
                .onItem().ifNull().failWith(VehicleNotFoundException::new)
                .flatMap(entity -> {
                    vehicleMapper.updateEntity(entity, request);
                    return vehicleRepository.persist(entity).map(vehicleMapper::toResponse);
                });
    }

    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return vehicleRepository.deleteByVehicleId(id)
                .flatMap(deleted -> {
                    if (deleted == 0) {
                        return Uni.createFrom().failure(new VehicleNotFoundException());
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
