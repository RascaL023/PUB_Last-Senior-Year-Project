package id.my.rascal.payment.internal.model.enums;


import com.fasterxml.jackson.annotation.JsonValue;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentProvider {
    INTERNAL("INTERNAL"),
    XENDIT("XENDIT");

    private final String provider;

    PaymentProvider(String provider) { this.provider = provider; }

    @JsonValue
    public String getProvider() {
        return provider;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PaymentProvider fromString(String value) {
        if (value == null) return null;

        String normalized = StringUtil.toUnderscoredEnum(value).toUpperCase();

        return switch (normalized) {
            case "INTERNAL", "CASH" -> INTERNAL;
            case "XENDIT" -> XENDIT;
            default -> throw new BadRequestException( 
                "Invalid payment provider: '" + value + "'. Allowed: " + allowedValues()
            );
        };
    }

    private static String allowedValues() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

}
