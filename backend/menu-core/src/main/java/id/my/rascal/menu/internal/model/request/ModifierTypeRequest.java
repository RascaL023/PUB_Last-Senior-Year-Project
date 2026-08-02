package id.my.rascal.menu.internal.model.request;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModifierTypeRequest(
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 3, max = 20, message = "Name are between 3 and 20 characters")
    String name,

    @NotNull(message = "Minimum selection cannot be null")
    @Min(value = 0, message = "Invalid minimum selection")
    Integer minSelection,

    @NotNull(message = "Maximum selection cannot be null")
    @Min(value = 1, message = "Invalid maximum selection")
    Integer maxSelection,

    @NotEmpty(message = "Options cannot be null")
    List<ModifierOptionRequest> options
) {}

