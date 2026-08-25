package id.my.rascal.auth.internal.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.entity.Role;
import id.my.rascal.auth.internal.entity.UserAuth;
import id.my.rascal.auth.internal.model.request.LoginRequest;
import id.my.rascal.auth.internal.model.response.LoginResponse;
import id.my.rascal.auth.internal.repository.UserAuthRepository;
import id.my.rascal.common.exception.BadRequestException;
import id.rascal.filter.service.JwtService;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl (
        JwtService jwtService,
        UserAuthRepository userAuthRepository,
        PasswordEncoder passwordEncoder
    ) { 
        this.jwtService = jwtService;
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // TODO: better exception response code
    public LoginResponse login(LoginRequest request) {
        UserAuth userAuth = userAuthRepository.findForLoginByEmail(request.email().toLowerCase().trim())
            .orElseThrow(() -> new BadRequestException("Username/password salah"));

        if (!passwordEncoder.matches(request.password(), userAuth.getHashedPassword()))
            throw new BadRequestException("Username/password salah");

        // TODO: handle auth(jwt/session)
        // Body => accessToken, Header => refreshToken
        Set<String> roles = userAuth.getRoles().stream()
            .map(Role::getName).collect(Collectors.toSet());

        Set<String> authorities = userAuth.getRoles().stream()
            .flatMap(r -> r.getAuthorities().stream())
            .map(Authority::getName).collect(Collectors.toSet());

        String accessToken = jwtService.generateToken(userAuth.getId().toString(), roles, authorities);
        return new LoginResponse(userAuth.getId(), userAuth.getEmail(), accessToken);
    }
    
}
