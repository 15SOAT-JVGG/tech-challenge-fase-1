package br.com.fiap.postech.soat16.customerauth.domain;

import java.util.Objects;

import br.com.fiap.postech.soat16.customerauth.domain.exception.InvalidCpfException;

/**
 * Mesma regra de dígito verificador do {@code Document} da API: a função recusa o CPF inválido antes
 * de abrir conexão com o banco, e o valor normalizado é o que a API grava na coluna {@code document}.
 */
public final class Cpf {

    private static final int LENGTH = 11;
    private static final int FIRST_CHECK_POSITION = 9;
    private static final int SECOND_CHECK_POSITION = 10;

    private final String digits;

    private Cpf(String digits) {
        this.digits = digits;
    }

    public static Cpf of(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.length() != LENGTH || allDigitsEqual(digits) || !checkDigitsMatch(digits)) {
            throw new InvalidCpfException();
        }
        return new Cpf(digits);
    }

    public String digits() {
        return digits;
    }

    private static boolean allDigitsEqual(String digits) {
        return digits.chars().distinct().count() == 1;
    }

    private static boolean checkDigitsMatch(String digits) {
        return checkDigit(digits, FIRST_CHECK_POSITION) == digitAt(digits, FIRST_CHECK_POSITION)
                && checkDigit(digits, SECOND_CHECK_POSITION) == digitAt(digits, SECOND_CHECK_POSITION);
    }

    private static int checkDigit(String digits, int length) {
        int sum = 0;
        int weight = length + 1;
        for (int index = 0; index < length; index++) {
            sum += digitAt(digits, index) * weight;
            weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int digitAt(String digits, int index) {
        return digits.charAt(index) - '0';
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Cpf other && digits.equals(other.digits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(digits);
    }

    /**
     * Mascarado de propósito: o CPF é dado pessoal e não deve aparecer inteiro em log.
     */
    @Override
    public String toString() {
        return "***." + digits.substring(3, 6) + ".***-" + digits.substring(9);
    }
}
