package br.com.fiap.postech.soat16.fase1.auth.adapter.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.auth.adapter.out.persistence.entity.AppUserJpaEntity;
import br.com.fiap.postech.soat16.fase1.auth.adapter.out.persistence.mapper.AppUserPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.auth.application.port.out.AppUserPersistencePort;
import br.com.fiap.postech.soat16.fase1.auth.domain.model.AppUser;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class AppUserRepository implements PanacheRepository<AppUserJpaEntity>, AppUserPersistencePort {

    @Override
    public Uni<AppUser> findByUsername(String username) {
        return find("username = ?1 and active = true", username).firstResult()
                .map(AppUserPersistenceMapper::toDomain);
    }

    @Override
    public Uni<Boolean> existsByUsername(String username) {
        return count("username = ?1", username).map(total -> total > 0);
    }

    @Override
    public Uni<AppUser> save(AppUser user) {
        if (user.getId() == null) {
            return persist(AppUserPersistenceMapper.toJpaEntity(user))
                    .map(AppUserPersistenceMapper::toDomain);
        }
        return findById(user.getId())
                .onItem().ifNotNull().transform(entity -> {
                    AppUserPersistenceMapper.copyState(user, entity);
                    return AppUserPersistenceMapper.toDomain(entity);
                });
    }
}
