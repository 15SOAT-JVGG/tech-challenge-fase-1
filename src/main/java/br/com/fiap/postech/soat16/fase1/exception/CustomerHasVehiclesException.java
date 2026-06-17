package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.CustomerErrorCode.CUSTOMER_HAS_VEHICLES;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.CONFLICT;

public class CustomerHasVehiclesException extends AppException {

    public CustomerHasVehiclesException() {
        super("Customer has vehicles associated and cannot be deleted", CUSTOMER_HAS_VEHICLES, CONFLICT);
    }
}
