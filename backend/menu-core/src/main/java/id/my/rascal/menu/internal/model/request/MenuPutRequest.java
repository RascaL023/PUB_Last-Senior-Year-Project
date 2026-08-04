package id.my.rascal.menu.internal.model.request;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MenuPutRequest(
    @NotBlank(message = "Menu name must be filled")
    @Size(min = 3, max = 30, message = "Menu name are between 3 to 30 characters")
    String name,

    @NotEmpty(message = "Category IDs cannot be empty")
    List<@Min(value = 1, message = "Invalid category ID") Long> categoryIds,

    String description,
    List<String> imageUrls,

    @NotNull(message = "Base price must be filled")
    @Min(value = 500, message = "Minimum price is 500")
    Integer basePrice,

    Boolean isAvailable,

    List<Long> ModifierTypeIds
) { }
