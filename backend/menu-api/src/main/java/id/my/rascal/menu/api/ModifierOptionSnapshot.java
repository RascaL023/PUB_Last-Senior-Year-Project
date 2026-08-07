package id.my.rascal.menu.api;

public record ModifierOptionSnapshot(
    Long id,
    Long modifierTypeId,
    String name,
    Integer additionalPrice
) {}
