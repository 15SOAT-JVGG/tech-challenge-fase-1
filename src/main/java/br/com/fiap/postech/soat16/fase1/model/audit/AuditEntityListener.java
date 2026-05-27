package br.com.fiap.postech.soat16.fase1.model.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ApplicationScoped
public class AuditEntityListener {

    private static final String SYSTEM_USER = "system";

    @PrePersist
    public void prePersist(AuditableEntity entity) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(SYSTEM_USER);
        entity.setUpdatedBy(SYSTEM_USER);
    }

    @PreUpdate
    public void preUpdate(AuditableEntity entity) {
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedBy(SYSTEM_USER);
    }
}
