package br.com.fiap.postech.soat16.fase1.service;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantCreateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.AttendantNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateAttendantEmailException;
import br.com.fiap.postech.soat16.fase1.exception.InactiveAttendantException;
import br.com.fiap.postech.soat16.fase1.exception.InvalidAttendantCredentialsException;
import br.com.fiap.postech.soat16.fase1.mapper.AttendantMapper;
import br.com.fiap.postech.soat16.fase1.repository.AttendantRepository;

import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class AttendantService {

    private final AttendantRepository repository;
    private final AttendantMapper mapper;
    private final PasswordService passwordService;

    public PageableResponseDto<AttendantResponseDto> findAll(String q, int page, int size) {
        var data = repository.findPage(page, size).stream().map(mapper::toResponse).toList();
        return PageableResponseDto.of(data, page, size, repository.count());
    }

    public AttendantResponseDto findById(UUID id) {
        return repository.findByAttendantId(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new AttendantNotFoundException(id));
    }

    @Transactional
    public void create(AttendantCreateRequestDto request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new DuplicateAttendantEmailException();
        }
        String passwordHash = passwordService.hash(request.getPassword());
        repository.persist(mapper.toEntity(request, passwordHash));
    }

    @Transactional
    public AttendantResponseDto update(UUID id, AttendantUpdateRequestDto request) {
        var entity = repository.findByAttendantId(id)
                .orElseThrow(() -> new AttendantNotFoundException(id));

        if (repository.existsByEmailAndDifferentId(request.getEmail(), id)) {
            throw new DuplicateAttendantEmailException();
        }

        mapper.updateEntity(entity, request);
        repository.persist(entity);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (repository.deleteByAttendantId(id) == 0) {
            throw new AttendantNotFoundException(id);
        }
    }

    public AttendantLoginResponseDto login(AttendantLoginRequestDto request) {
        var entity = repository.findByEmail(request.getEmail())
                .orElseThrow(InvalidAttendantCredentialsException::new);

        if (!entity.isActive()) {
            throw new InactiveAttendantException();
        }

        if (!passwordService.matches(request.getPassword(), entity.getPasswordHash())) {
            throw new InvalidAttendantCredentialsException();
        }

        return mapper.toLoginResponse(entity);
    }
}
