package id.my.rascal.auth.internal.model.response;

import java.time.LocalDateTime;
import java.util.Set;

public record UserAuthResponse(
    Long id,
    String email,
    Set<RoleResponse> roles,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
