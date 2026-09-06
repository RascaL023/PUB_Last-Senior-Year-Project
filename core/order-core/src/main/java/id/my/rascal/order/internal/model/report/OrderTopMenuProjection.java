package id.my.rascal.order.internal.model.report;

public record OrderTopMenuProjection(
    Long menuId,
    String itemName,
    Long qty,
    Long revenue
) {}