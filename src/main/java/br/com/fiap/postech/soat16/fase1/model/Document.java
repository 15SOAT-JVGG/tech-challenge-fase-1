package br.com.fiap.postech.soat16.fase1.model;

import java.util.Objects;

import br.com.fiap.postech.soat16.fase1.exception.InvalidDocumentException;
import br.com.fiap.postech.soat16.fase1.model.enums.DocumentType;

public final class Document {

    private static final int CPF_LENGTH = 11;
    private static final int CNPJ_LENGTH = 14;

    private final String value;
    private final DocumentType type;

    private Document(String value, DocumentType type) {
        this.value = value;
        this.type = type;
    }

    public static Document of(String raw) {
        String digits = normalize(raw);

        if (digits.length() == CPF_LENGTH && isValidCpf(digits)) {
            return new Document(digits, DocumentType.CPF);
        }
        if (digits.length() == CNPJ_LENGTH && isValidCnpj(digits)) {
            return new Document(digits, DocumentType.CNPJ);
        }
        throw new InvalidDocumentException(raw);
    }

    public static boolean isValid(String raw) {
        try {
            of(raw);
            return true;
        } catch (InvalidDocumentException e) {
            return false;
        }
    }

    public String getValue() {
        return value;
    }

    public DocumentType getType() {
        return type;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\D", "");
    }

    private static boolean allDigitsEqual(String digits) {
        return digits.chars().distinct().count() == 1;
    }

    private static boolean isValidCpf(String cpf) {
        if (allDigitsEqual(cpf)) {
            return false;
        }
        int firstCheck = checkDigit(cpf, 9, 10);
        int secondCheck = checkDigit(cpf, 10, 11);
        return firstCheck == digitAt(cpf, 9) && secondCheck == digitAt(cpf, 10);
    }

    private static boolean isValidCnpj(String cnpj) {
        if (allDigitsEqual(cnpj)) {
            return false;
        }
        int[] firstWeights = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] secondWeights = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int firstCheck = checkDigit(cnpj, firstWeights);
        int secondCheck = checkDigit(cnpj, secondWeights);
        return firstCheck == digitAt(cnpj, 12) && secondCheck == digitAt(cnpj, 13);
    }

    private static int checkDigit(String digits, int length, int startWeight) {
        int sum = 0;
        int weight = startWeight;
        for (int i = 0; i < length; i++) {
            sum += digitAt(digits, i) * weight;
            weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int checkDigit(String digits, int... weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += digitAt(digits, i) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int digitAt(String digits, int index) {
        return digits.charAt(index) - '0';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Document other)) {
            return false;
        }
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
