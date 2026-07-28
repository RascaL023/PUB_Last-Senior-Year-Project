package id.my.rascal.auth.internal.model.request;

import java.util.Optional;
import java.util.Set;

public record UserAuthPatchRequest(
    Optional<String> email,
    Optional<String> password,
    Optional<Set<Long>> roleIds
) {
    public UserAuthPatchRequest(String email, String password, Set<Long> roleIds) {
        this(Optional.ofNullable(email), Optional.ofNullable(password), Optional.ofNullable(roleIds));
    }

    public boolean isEmptyPatch() {
        return (email == null || email.isEmpty())
            && (password == null || password.isEmpty())
            && (roleIds == null || roleIds.isEmpty());
    }
}
