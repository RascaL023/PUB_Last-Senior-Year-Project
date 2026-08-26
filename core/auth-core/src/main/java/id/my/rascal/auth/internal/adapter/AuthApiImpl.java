package id.my.rascal.auth.internal.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.api.AuthApi;
import id.my.rascal.auth.api.UserAuthApiResponse;
import id.my.rascal.auth.internal.repository.UserAuthRepository;

@Component
public class AuthApiImpl implements AuthApi {

    private final UserAuthRepository userAuthRepository;

    public AuthApiImpl(UserAuthRepository userAuthRepository) {
        this.userAuthRepository = userAuthRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthApiResponse> getById(Long id) {
        return userAuthRepository.findActiveById(id)
            .map(u -> new UserAuthApiResponse(u.getId(), u.getEmail()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthApiResponse> getByEmail(String email) {
        return userAuthRepository.findActiveByEmail(email)
            .map(u -> new UserAuthApiResponse(u.getId(), u.getEmail()));
    }
    
}
