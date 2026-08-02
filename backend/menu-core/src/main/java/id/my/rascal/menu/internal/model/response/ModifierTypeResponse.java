package id.my.rascal.menu.internal.model.response;

import java.util.List;

public record ModifierTypeResponse(
    Long id,
    String name,
    Integer minSelection,
    Integer maxSelection,
    List<ModifierOptionResponse> options
) {}
