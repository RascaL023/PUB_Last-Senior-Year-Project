package id.my.rascal.menu.internal.model.response;

public record MenuCategoryResponse(
    Long id,
    String displayName,
    String categoryCode,
    int displayOrder
) { }