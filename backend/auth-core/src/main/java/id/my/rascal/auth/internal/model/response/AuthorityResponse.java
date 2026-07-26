package id.my.rascal.auth.internal.model.response;

import java.time.LocalDateTime;

public record AuthorityResponse(
    Long id,
    String name,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
