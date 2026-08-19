package id.my.rascal.menu.internal.model.response;

import java.time.LocalDateTime;
import java.util.List;

public record MenuResponse(
    Long id,
    String name,
    String description,
    List<MenuCategoryResponse> categories,
    List<String> imageUrls,
    Integer basePrice,
    Boolean isAvailable,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<ModifierTypeResponse> modifierTypes
) {
}
