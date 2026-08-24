package id.my.rascal.payment.internal.model.response;

import java.time.LocalDateTime;

public record PaymentMethodResponse(
    Long id,
    String code,
    String name,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
