package br.com.fiap.postech.soat16.fase1.worker.domain.model;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.shared.domain.model.audit.AuditableEntity;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Worker extends AuditableEntity {

    @EqualsAndHashCode.Include
    private UUID id;

    private WorkerProfile profile;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private String passwordHash;

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
