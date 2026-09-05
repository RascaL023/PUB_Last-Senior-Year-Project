package id.my.rascal.menu.internal.model.search;

import id.my.rascal.common.exception.BadRequestException;

public enum DeletedScope {
    ACTIVE,
    DELETED,
    ALL;

    public static DeletedScope from(String value) {
        if (value == null || value.isBlank()) return ACTIVE;

        return switch (value.trim().toLowerCase()) {
            case "active" -> ACTIVE;
            case "deleted" -> DELETED;
            case "all" -> ALL;
            default -> throw new BadRequestException(
                "Invalid 'deleted' parameter: '" + value + "'. Expected one of: active, deleted, all"
            );
        };
    }
}
