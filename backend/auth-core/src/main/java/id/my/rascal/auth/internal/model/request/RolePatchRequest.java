package id.my.rascal.auth.internal.model.request;

import java.util.Optional;
import java.util.Set;

public record RolePatchRequest(
    Optional<String> name,
    Optional<Set<Long>> authorityIds
) {
    public RolePatchRequest(String name, Set<Long> authorityIds) {
        this(Optional.ofNullable(name), Optional.ofNullable(authorityIds));
    }
}
