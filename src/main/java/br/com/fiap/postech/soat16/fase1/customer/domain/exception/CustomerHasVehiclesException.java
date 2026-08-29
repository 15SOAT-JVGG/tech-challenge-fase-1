package br.com.fiap.postech.soat16.fase1.customer.domain.exception;

import static br.com.fiap.postech.soat16.fase1.customer.domain.exception.CustomerErrorCode.CUSTOMER_HAS_VEHICLES;
import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.CONFLICT;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class CustomerHasVehiclesException extends AppException {

    public CustomerHasVehiclesException() {
        super("Customer has vehicles associated and cannot be deleted", CUSTOMER_HAS_VEHICLES, CONFLICT);
    }
}
