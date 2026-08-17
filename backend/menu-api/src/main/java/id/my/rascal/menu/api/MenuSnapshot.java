package id.my.rascal.menu.api;

import java.util.List;

public record MenuSnapshot(
    Long id,
    String name,
    Integer basePrice,
    boolean isAvailable,
    List<ModifierTypeSnapshot> modifierTypes
) {}
