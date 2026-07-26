package id.my.rascal.auth.api;

import java.util.Optional;

public interface AuthApi {
    Optional<UserAuthResponse> getById(Long id);
    Optional<UserAuthResponse> getByEmail(String email);
}
