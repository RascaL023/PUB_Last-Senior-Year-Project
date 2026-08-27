package id.my.rascal.dining.internal.model.response;

import java.time.LocalDateTime;
import java.util.List;

import id.my.rascal.dining.internal.entity.DiningStatus;

public record DiningResponse(
    Long id,
    Long tableId,
    String tableNumber,
    DiningStatus status,
    Integer totalPrice,
    List<DiningOrderSummary> orders,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime closedAt
) {}
