package id.my.rascal.auth.internal.model.request;

import java.util.Set;

public record UserAuthPutRequest(
    String email,
    String password,
    Set<Long> roleIds
) {}
