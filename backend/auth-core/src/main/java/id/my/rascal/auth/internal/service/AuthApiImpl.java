package id.my.rascal.auth.internal.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.api.AuthApi;
import id.my.rascal.auth.api.UserAuthResponse;
import id.my.rascal.auth.internal.repository.UserAuthRepository;

@Service
public class AuthApiImpl implements AuthApi {

    private final UserAuthRepository userAuthRepository;

    public AuthApiImpl(UserAuthRepository userAuthRepository) {
        this.userAuthRepository = userAuthRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthResponse> getById(Long id) {
        return userAuthRepository.findById(id)
            .map(u -> new UserAuthResponse(u.getId(), u.getEmail()));
    }

    @Override
    public Optional<UserAuthResponse> getByEmail(String email) {
        return userAuthRepository.findByEmail(email)
            .map(u -> new UserAuthResponse(u.getId(), u.getEmail()));
    }
    
}
