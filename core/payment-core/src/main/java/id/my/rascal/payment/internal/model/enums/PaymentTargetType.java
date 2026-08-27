package id.my.rascal.payment.internal.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentTargetType {
    ORDER("ORDER"),
    DINE_IN("DINE_IN");

    private final String targetType;

    PaymentTargetType(String targetType) {
        this.targetType = targetType;
    }

    @JsonValue
    public String getTargetType() {
        return targetType;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PaymentTargetType fromString(String value) {
        if (value == null) return null;

        String normalized = StringUtil.toUnderscoredEnum(value).toUpperCase();

        return switch (normalized) {
            case "ORDER" -> ORDER;
            case "DINEIN" -> DINE_IN;
            case "DINE_IN" -> DINE_IN;
            default -> throw new BadRequestException( 
                "Invalid PaymentTargetType: '" + value + "'. Allowed: " + allowedValues()
            );
        };
    }

    private static String allowedValues() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

}
