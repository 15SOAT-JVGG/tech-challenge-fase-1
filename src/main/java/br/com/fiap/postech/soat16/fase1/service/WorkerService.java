package br.com.fiap.postech.soat16.fase1.service;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkerLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkerLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkerResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.WorkerNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateWorkerEmailException;
import br.com.fiap.postech.soat16.fase1.exception.InactiveWorkerException;
import br.com.fiap.postech.soat16.fase1.exception.InvalidWorkerCredentialsException;
import br.com.fiap.postech.soat16.fase1.mapper.WorkerMapper;
import br.com.fiap.postech.soat16.fase1.repository.WorkerRepository;

import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository repository;
    private final WorkerMapper mapper;
    private final PasswordService passwordService;

    public PageableResponseDto<WorkerResponseDto> findAll(String q, int page, int size) {
        var data = repository.findPage(page, size).stream().map(mapper::toResponse).toList();
        return PageableResponseDto.of(data, page, size, repository.count());
    }

    public WorkerResponseDto findById(UUID id) {
        return repository.findByWorkerId(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new WorkerNotFoundException(id));
    }

    @Transactional
    public void create(WorkerRequestDto request) {
        if (repository.existsByEmail(request.email())) {
            throw new DuplicateWorkerEmailException();
        }
        String passwordHash = passwordService.hash(request.password());
        repository.persist(mapper.toEntity(request, passwordHash));
    }

    @Transactional
    public WorkerResponseDto update(UUID id, WorkerRequestDto request) {
        var entity = repository.findByWorkerId(id)
                .orElseThrow(() -> new WorkerNotFoundException(id));

        if (repository.existsByEmailAndDifferentId(request.email(), id)) {
            throw new DuplicateWorkerEmailException();
        }

        mapper.updateEntity(entity, request);
        repository.persist(entity);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (repository.deleteByWorkerId(id) == 0) {
            throw new WorkerNotFoundException(id);
        }
    }

    public WorkerLoginResponseDto login(WorkerLoginRequestDto request) {
        var entity = repository.findByEmail(request.email())
                .orElseThrow(InvalidWorkerCredentialsException::new);

        if (!entity.isActive()) {
            throw new InactiveWorkerException();
        }

        if (!passwordService.matches(request.password(), entity.getPasswordHash())) {
            throw new InvalidWorkerCredentialsException();
        }

        return mapper.toLoginResponse(entity);
    }
}
