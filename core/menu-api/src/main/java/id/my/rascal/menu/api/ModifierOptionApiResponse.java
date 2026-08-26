package id.my.rascal.menu.api;

public record ModifierOptionApiResponse(
    Long id,
    Long modifierTypeId,
    String name,
    Integer additionalPrice
) {}
