package br.com.fiap.postech.soat16.fase1.exception;

public enum EstimateErrorCode implements ErrorCode {

    ESTIMATE_NOT_FOUND,
    ESTIMATE_ALREADY_DECIDED,
    ESTIMATE_PART_NOT_FOUND;

    @Override
    public String getCode() {
        return name();
    }
}
