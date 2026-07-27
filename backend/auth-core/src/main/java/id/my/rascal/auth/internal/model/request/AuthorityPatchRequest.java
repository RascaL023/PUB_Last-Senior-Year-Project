package id.my.rascal.auth.internal.model.request;

import java.util.Optional;

public record AuthorityPatchRequest(
    Optional<String> name
) {
    public AuthorityPatchRequest(String name) {
        this(Optional.ofNullable(name));
    }

    public boolean isEmptyPatch() {
        return name.isEmpty();
    }
}
