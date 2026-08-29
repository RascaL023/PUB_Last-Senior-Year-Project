package id.my.rascal.order.internal.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OrderType {
    DINE_IN("DINE_IN"),
    TAKEAWAY("TAKEAWAY");

    private final String type;

    OrderType(String type) {
        this.type = type;
    }

    @JsonValue
    public String getTargetType() {
        return type;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OrderType fromString(String value) {
        if (value == null) return null;

        String normalized = StringUtil.toUnderscoredEnum(value).toUpperCase();

        return switch (normalized) {
            case "TAKEAWAY" -> TAKEAWAY;
            case "TAKE_AWAY" -> TAKEAWAY;
            case "DINE_IN" -> DINE_IN;
            default -> throw new BadRequestException( 
                "Invalid order type: '" + value + "'. Allowed: " + allowedValues()
            );
        };
    }

    private static String allowedValues() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

}
