package id.my.rascal.xendit.internal.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentStatus {
    PENDING("PENDING"),
    PAID("PAID"),
    FAILED("FAILED"),
    EXPIRED("EXPIRED"),
    REFUNDED("REFUNDED");

    private final String status;

    PaymentStatus(String status) {
        this.status = status;
    }

    @JsonValue
    public String getTargetType() {
        return status;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PaymentStatus fromString(String value) {
        if (value == null) return null;

        String normalized = StringUtil.toUnderscoredEnum(value).toUpperCase();

        return switch (normalized) {
            case "PENDING" -> PENDING;
            case "PAID" -> PAID;
            case "FAILED" -> FAILED;
            case "EXPIRED" -> EXPIRED;
            case "REFUNDED" -> REFUNDED;
            default -> throw new BadRequestException( 
                "Invalid payment status: '" + value + "'. Allowed: " + allowedValues()
            );
        };
    }

    private static String allowedValues() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

}
