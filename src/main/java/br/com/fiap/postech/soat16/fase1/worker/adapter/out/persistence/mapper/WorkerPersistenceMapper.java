package br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence.mapper;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence.AuditPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence.entity.WorkerJpaEntity;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;

public final class WorkerPersistenceMapper {

    private WorkerPersistenceMapper() {
    }

    public static Worker toDomain(WorkerJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        var worker = new Worker();
        worker.setId(entity.getId());
        worker.setProfile(entity.getProfile());
        worker.setFirstName(entity.getFirstName());
        worker.setLastName(entity.getLastName());
        worker.setEmail(entity.getEmail());
        worker.setPhoneNumber(entity.getPhoneNumber());
        worker.setPasswordHash(entity.getPasswordHash());
        worker.setActive(entity.isActive());
        AuditPersistenceMapper.copyToDomain(entity, worker);
        return worker;
    }

    public static WorkerJpaEntity toJpaEntity(Worker worker) {
        if (worker == null) {
            return null;
        }
        var entity = new WorkerJpaEntity();
        entity.setId(worker.getId());
        copyState(worker, entity);
        AuditPersistenceMapper.copyToJpaEntity(worker, entity);
        return entity;
    }

    /**
     * Referência somente com identidade, usada para preencher chaves estrangeiras sem carregar o
     * agregado completo de outro contexto.
     */
    public static WorkerJpaEntity toJpaReference(UUID workerId) {
        if (workerId == null) {
            return null;
        }
        var entity = new WorkerJpaEntity();
        entity.setId(workerId);
        return entity;
    }

    public static void copyState(Worker source, WorkerJpaEntity target) {
        target.setProfile(source.getProfile());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setEmail(source.getEmail());
        target.setPhoneNumber(source.getPhoneNumber());
        target.setPasswordHash(source.getPasswordHash());
        target.setActive(source.isActive());
    }
}
