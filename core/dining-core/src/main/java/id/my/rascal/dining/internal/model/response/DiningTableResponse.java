package id.my.rascal.dining.internal.model.response;

import java.time.LocalDateTime;

import id.my.rascal.dining.internal.entity.TableStatus;

public record DiningTableResponse(
    Long id,
    String tableNumber,
    TableStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
