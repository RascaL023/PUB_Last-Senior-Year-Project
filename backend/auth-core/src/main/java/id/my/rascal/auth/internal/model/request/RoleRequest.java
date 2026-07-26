package id.my.rascal.auth.internal.model.request;

import java.util.Set;

public record RoleRequest(
    String name,
    Set<Long> authorityIds
) {}
