package br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.shared.domain.model.audit.AuditableEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceItem extends AuditableEntity {

    private UUID id;

    private String name;

    private String description;

    private BigDecimal basePrice;

    private Integer estimatedDurationMinutes;

    private boolean active;

    /**
     * Identidade só existe após a persistência: duas instâncias ainda sem id nunca são iguais.
     */
    @Override
    public final boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        ServiceItem that = (ServiceItem) object;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
