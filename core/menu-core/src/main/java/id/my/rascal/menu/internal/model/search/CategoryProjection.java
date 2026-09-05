package id.my.rascal.menu.internal.model.search;

public record CategoryProjection(
    Long id,
    String name,
    String categoryCode,
    int displayOrder
) {}
