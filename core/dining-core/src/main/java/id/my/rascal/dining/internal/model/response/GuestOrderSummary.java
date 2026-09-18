package id.my.rascal.dining.internal.model.response;

import java.time.LocalDateTime;

public record GuestOrderSummary(
    String status,
    Integer totalPrice,
    LocalDateTime createdAt
) {}
