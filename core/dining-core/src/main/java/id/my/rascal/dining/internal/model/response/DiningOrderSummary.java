package id.my.rascal.dining.internal.model.response;

import java.time.LocalDateTime;

public record DiningOrderSummary(
    Long id,
    String orderNumber,
    String status,
    Integer totalPrice,
    LocalDateTime createdAt
) {}
