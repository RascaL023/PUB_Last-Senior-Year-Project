package id.my.rascal.menu.internal.model.response;

public record MenuCategoryResponse(
    Long id,
    String name,
    String categoryCode,
    int displayOrder
) { }
