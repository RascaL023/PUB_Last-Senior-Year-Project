package id.my.rascal.auth.internal.model.request;

import java.util.Optional;
import java.util.Set;

public record RolePatchRequest(
    Long id,
    Optional<String> name,
    Optional<Set<Long>> authorityIds
) {
    public RolePatchRequest(Long id, String name, Set<Long> authorityIds) {
        this(id, Optional.ofNullable(name), Optional.ofNullable(authorityIds));
    }
}
