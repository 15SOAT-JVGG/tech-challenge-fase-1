package br.com.fiap.postech.soat16.fase1.model;

import br.com.fiap.postech.soat16.fase1.model.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "customer", schema = "oficina-mecanica")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class Customer extends AuditableEntity {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "document", nullable = false, unique = true)
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;
}
