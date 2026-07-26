package id.my.rascal.auth.internal.model.request;

import java.util.Set;

public record UserAuthRequest(
    String email,
    String password,
    Set<Long> roleIds
) {}
