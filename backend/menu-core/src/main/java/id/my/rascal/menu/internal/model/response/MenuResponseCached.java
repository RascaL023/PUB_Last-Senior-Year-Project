package id.my.rascal.menu.internal.model.response;

import java.util.Set;

public record MenuResponseCached(
    Long id,
    Set<Long> categoryIds,
    Set<Long> modifierTypesIds
) {}
