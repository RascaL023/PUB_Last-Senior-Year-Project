package id.my.rascal.dining.api;

import java.time.LocalDateTime;

public record DiningApiResponse(
    Long id,
    Long tableId,
    String tableNumber,
    String status,
    Integer totalPrice,
    LocalDateTime createdAt
) {}
