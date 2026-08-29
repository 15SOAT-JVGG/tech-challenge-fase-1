package br.com.fiap.postech.soat16.fase1.auth.adapter.out.persistence.mapper;

import br.com.fiap.postech.soat16.fase1.auth.adapter.out.persistence.entity.AppUserJpaEntity;
import br.com.fiap.postech.soat16.fase1.auth.domain.model.AppUser;

public final class AppUserPersistenceMapper {

    private AppUserPersistenceMapper() {
    }

    public static AppUser toDomain(AppUserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return AppUser.restore(
                entity.getId(),
                entity.getUsername(),
                entity.getPassword(),
                entity.getRole(),
                entity.getActive(),
                entity.getCreatedAt());
    }

    public static AppUserJpaEntity toJpaEntity(AppUser user) {
        if (user == null) {
            return null;
        }
        var entity = new AppUserJpaEntity();
        entity.setId(user.getId());
        entity.setCreatedAt(user.getCreatedAt());
        copyState(user, entity);
        return entity;
    }

    public static void copyState(AppUser source, AppUserJpaEntity target) {
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setRole(source.getRole());
        target.setActive(source.getActive());
    }
}
