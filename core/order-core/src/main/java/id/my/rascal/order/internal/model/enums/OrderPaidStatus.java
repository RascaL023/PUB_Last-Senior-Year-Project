package id.my.rascal.order.internal.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OrderPaidStatus {
    UNPAID("UNPAID"),
    PAID("PAID");

    private final String paidStatus;

    OrderPaidStatus(String paidStatus) {
        this.paidStatus = paidStatus;
    }

    @JsonValue
    public String getPaidStatus() {
        return paidStatus;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OrderPaidStatus fromString(String value) {
        if (value == null) return null;

        String normalized = StringUtil.toUnderscoredEnum(value).toUpperCase();

        return switch (normalized) {
            case "UNPAID" -> UNPAID;
            case "UN_PAID" -> UNPAID;
            case "PAID"  -> PAID;
            default -> throw new BadRequestException( 
                "Invalid order paid status: '" + value + "'. Allowed: " + allowedValues()
            );
        };
    }

    private static String allowedValues() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

}
