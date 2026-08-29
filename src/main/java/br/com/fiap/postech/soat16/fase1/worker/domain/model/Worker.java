package br.com.fiap.postech.soat16.fase1.worker.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import br.com.fiap.postech.soat16.fase1.shared.domain.model.audit.AuditableEntity;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "worker", schema = "oficina_mecanica")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class Worker extends AuditableEntity {

    @EqualsAndHashCode.Include
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

    public static Worker create(WorkerProfile profile, String firstName, String lastName,
            String email, String phoneNumber, String passwordHash) {
        var worker = new Worker();
        worker.profile = profile;
        worker.firstName = firstName;
        worker.lastName = lastName;
        worker.email = email;
        worker.phoneNumber = phoneNumber;
        worker.passwordHash = passwordHash;
        worker.active = true;
        return worker;
    }

    public void update(WorkerProfile profile, String firstName, String lastName,
            String email, String phoneNumber) {
        this.profile = profile;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }
}
