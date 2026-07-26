package id.my.rascal.auth.internal.model.request;

import java.util.Set;

public record RolePutRequest(
    Long id,
    String name,
    Set<Long> authorityIds
) {}
