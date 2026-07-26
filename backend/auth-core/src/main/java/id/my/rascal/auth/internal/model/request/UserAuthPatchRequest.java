package id.my.rascal.auth.internal.model.request;

import java.util.Optional;
import java.util.Set;

public record UserAuthPatchRequest(
    Long id,
    Optional<String> email,
    Optional<String> password,
    Optional<Set<Long>> roleIds
) {
    public UserAuthPatchRequest(Long id, String email, String password, Set<Long> roleIds) {
        this(id, Optional.ofNullable(email), Optional.ofNullable(password), Optional.ofNullable(roleIds));
    }
}
