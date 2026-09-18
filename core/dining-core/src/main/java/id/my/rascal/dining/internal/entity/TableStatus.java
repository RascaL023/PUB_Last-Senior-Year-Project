package id.my.rascal.dining.internal.entity;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;

public enum TableStatus {
    AVAILABLE,
    OCCUPIED;

    public static TableStatus fromString(String value) {
        if (StringUtil.safeIsBlank(value)) return null;

        String normalized = StringUtil.toUnderscoredEnum(value).toUpperCase();

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                "Invalid table status: '" + value + "'. Allowed: AVAILABLE, OCCUPIED"
            );
        }
    }

}
