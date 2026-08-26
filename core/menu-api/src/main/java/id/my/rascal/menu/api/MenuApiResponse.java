package id.my.rascal.menu.api;

import java.util.List;

public record MenuApiResponse(
    Long id,
    String name,
    Integer basePrice,
    boolean isAvailable,
    List<ModifierTypeApiResponse> modifierTypes
) {}
