package id.my.rascal.menu.internal.model.search;

import java.util.List;

public record ModifierTypeProjection(
    Long id,
    String name,
    Integer minSelection,
    Integer maxSelection,
    List<ModifierOptionProjection> options
) {}
