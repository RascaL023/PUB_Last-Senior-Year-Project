package id.my.rascal.auth.internal.model.request;

import java.util.Optional;

public record AuthorityPatchRequest(
    Long id,
    Optional<String> name
) {
    public AuthorityPatchRequest(Long id, String name) {
        this(id, Optional.ofNullable(name));
    }
}
