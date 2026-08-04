package id.my.rascal.menu.internal.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MenuCategoryPutRequest(
    @NotBlank(message = "Display name cannot be blank")
    @Size(min = 3, max = 30, message = "Display name are between 3 and 30 characters")
    String displayName,

    @NotBlank(message = "Category code cannot be blank")
    @Size(min = 3, max = 30, message = "Category code are between 3 and 30 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Category code must be lowercase slug (e.g. hot-drinks)")
    String categoryCode,

    @NotNull(message = "Display order cannot be null")
    @Min(value = 0, message = "Invalid display order")
    Integer displayOrder
) { }