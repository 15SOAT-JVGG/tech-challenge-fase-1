package br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence.AuditableJpaEntity;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "Worker")
@Table(name = "worker", schema = "oficina_mecanica")
@Getter
@Setter
public class WorkerJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "worker_id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkerProfile profile;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @Column(name = "active", nullable = false)
    private boolean active;
}
