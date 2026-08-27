package id.my.rascal.dining.internal.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OpenDiningRequest(
    @NotNull(message = "Table ID is required")
    @Min(value = 1, message = "Invalid table ID")
    Long tableId
) {}
