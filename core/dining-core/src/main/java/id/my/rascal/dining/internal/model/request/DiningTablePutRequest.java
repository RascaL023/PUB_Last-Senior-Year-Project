package id.my.rascal.dining.internal.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiningTablePutRequest(
    @NotBlank(message = "Table number is required")
    @Size(max = 50, message = "Table number cannot exceed 50 characters")
    String tableNumber
) {}
