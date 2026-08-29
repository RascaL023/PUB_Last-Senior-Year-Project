package id.my.rascal.order.internal.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OrderStatus {
    CREATED("CREATED"),
    CONFIRMED("CONFIRMED"),
    PREPARING("PREPARING"),
    READY("READY"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String status;

    OrderStatus(String status) {
        this.status = status;
    }

    @JsonValue
    public String getStatus() {
        return status;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OrderStatus fromString(String value) {
        if (value == null) return null;

        String normalized = StringUtil.toUnderscoredEnum(value).toUpperCase();

        return switch (normalized) {
            case "CREATED" -> CREATED;
            case "CREATE"  -> CREATED;
            case "CONFIRMED" -> CONFIRMED;
            case "PREPARING" -> PREPARING;
            case "PREPARE" -> PREPARING;
            case "READY" -> READY;
            case "COMPLETED" -> COMPLETED;
            case "COMPLETE" -> COMPLETED;
            case "CANCELLED" -> CANCELLED;
            case "CANCEL" -> CANCELLED;
            default -> throw new BadRequestException( 
                "Invalid order status: '" + value + "'. Allowed: " + allowedValues()
            );
        };
    }

    private static String allowedValues() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

}
