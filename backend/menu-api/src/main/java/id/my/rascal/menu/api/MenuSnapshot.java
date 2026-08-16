package id.my.rascal.menu.api;

import java.util.List;

public record MenuSnapshot(
    Long id,
    String name,
    Integer basePrice,
    List<ModifierTypeSnapshot> modifierTypes
) {}
