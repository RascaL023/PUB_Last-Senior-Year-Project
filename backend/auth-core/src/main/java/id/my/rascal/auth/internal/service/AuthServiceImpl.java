package id.my.rascal.auth.internal.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import id.my.rascal.auth.internal.entity.UserAuth;
import id.my.rascal.auth.internal.model.request.LoginRequest;
import id.my.rascal.auth.internal.model.response.LoginResponse;
// import id.my.rascal.auth.internal.repository.AuthorityRepository;
// import id.my.rascal.auth.internal.repository.RoleRepository;
import id.my.rascal.auth.internal.repository.UserAuthRepository;
import id.my.rascal.common.exception.BadRequestException;

@Service
public class AuthServiceImpl implements AuthService {

    // private final AuthorityRepository authorityRepository;
    // private final RoleRepository roleRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl (
        // AuthorityRepository authorityRepository,
        // RoleRepository roleRepository,
        UserAuthRepository userAuthRepository,
        PasswordEncoder passwordEncoder
    ) { 
        // this.authorityRepository = authorityRepository;
        // this.roleRepository = roleRepository;
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // TODO: better exception response code
    public LoginResponse login(LoginRequest request) {
        UserAuth userAuth = userAuthRepository.findByEmail(request.email().toLowerCase().trim())
            .orElseThrow(() -> new BadRequestException("Username/password salah"));

        if (!passwordEncoder.matches(request.password(), userAuth.getHashedPassword()))
            throw new BadRequestException("Username/password salah");

        // TODO: handle auth(jwt/session)
        // Body => accessToken, Header => refreshToken
        String accessToken = "Ini akses token ceritanya";

        return new LoginResponse(userAuth.getId(), userAuth.getEmail(), accessToken);
    }
    
}
