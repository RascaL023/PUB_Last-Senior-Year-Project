package id.my.rascal.auth.api;

import java.util.Optional;

public interface AuthApi {

    Optional<UserAuthApiResponse> getById(Long id);
    Optional<UserAuthApiResponse> getByEmail(String email);
    UserAuthApiResponse createAccount(CreateAccountRequest request);
    void softDeleteAccount(Long userAuthId);

    /**
     * Ganti peran (role) akun login. Dipakai saat HR mengubah role karyawan.
     * Semua refresh token akun dicabut agar otoritas lama tidak dipakai lagi.
     */
    UserAuthApiResponse updateAccountRole(Long userAuthId, String roleName);

}
