package id.my.rascal.auth.internal.model.response;

import java.time.LocalDateTime;
import java.util.Set;

public record RoleResponse(
    Long id,
    String name,
    Set<AuthorityResponse> authorities,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
