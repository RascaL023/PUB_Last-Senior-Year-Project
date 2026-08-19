package id.my.rascal.menu.internal.model.request;

import jakarta.validation.constraints.*;

public record ModifierOptionRequest(
    @NotBlank(message = "Option name must be filled")
    String name,

    @NotNull(message = "Additional price cannot be null")
    @Min(value = 0, message = "Minimum additional price is 0")
    Integer additionalPrice
) {}
