package id.my.rascal.customer.internal.model.response;

import java.time.LocalDateTime;

public record CustomerResponse(
    Long id,
    Long userAuthId,
    String name,
    String email,
    String phone,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
