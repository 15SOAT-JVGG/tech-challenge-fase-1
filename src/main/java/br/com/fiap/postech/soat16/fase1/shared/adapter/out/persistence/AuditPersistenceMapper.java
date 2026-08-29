package br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence;

import br.com.fiap.postech.soat16.fase1.shared.domain.model.audit.AuditableEntity;

public final class AuditPersistenceMapper {

    private AuditPersistenceMapper() {
    }

    public static void copyToDomain(AuditableJpaEntity source, AuditableEntity target) {
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setCreatedBy(source.getCreatedBy());
        target.setUpdatedBy(source.getUpdatedBy());
    }

    public static void copyToJpaEntity(AuditableEntity source, AuditableJpaEntity target) {
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setCreatedBy(source.getCreatedBy());
        target.setUpdatedBy(source.getUpdatedBy());
    }
}
