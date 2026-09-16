package id.my.rascal.auth.api;

import java.util.Optional;

public interface AuthApi {

    Optional<UserAuthApiResponse> getById(Long id);
    Optional<UserAuthApiResponse> getByEmail(String email);
    UserAuthApiResponse createAccount(CreateAccountRequest request);
    void softDeleteAccount(Long userAuthId);

}
