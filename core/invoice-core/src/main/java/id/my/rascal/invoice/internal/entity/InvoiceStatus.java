package id.my.rascal.invoice.internal.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum InvoiceStatus {
    OPEN("OPEN"),
    PARTIALLY_PAID("PARTIALLY_PAID"),
    PAID("PAID"),
    VOID("VOID");

    private final String status;

    InvoiceStatus(String status) {
        this.status = status;
    }

    @JsonValue
    public String getStatus() {
        return status;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static InvoiceStatus fromString(String value) {
        if (value == null) return null;

        String normalized = StringUtil.toUnderscoredEnum(value).toUpperCase();

        return switch (normalized) {
            case "OPEN" -> OPEN;
            case "PARTIALLY_PAID", "PARTIALLYPAID", "PARTIAL", "PARTIALLY" -> PARTIALLY_PAID;
            case "PAID", "SETTLED" -> PAID;
            case "VOID", "VOIDED", "CANCELLED", "CANCEL" -> VOID;
            default -> throw new BadRequestException(
                "Invalid invoice status: '" + value + "'. Allowed: " + allowedValues()
            );
        };
    }

    private static String allowedValues() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

}
