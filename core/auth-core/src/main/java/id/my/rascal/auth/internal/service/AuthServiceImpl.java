package id.my.rascal.auth.internal.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.entity.RefreshToken;
import id.my.rascal.auth.internal.entity.Role;
import id.my.rascal.auth.internal.entity.UserAuth;
import id.my.rascal.auth.internal.model.request.LoginRequest;
import id.my.rascal.auth.internal.model.response.LoginResultResponse;
import id.my.rascal.auth.internal.model.response.RefreshResultResponse;
import id.my.rascal.auth.internal.repository.RefreshTokenRepository;
import id.my.rascal.auth.internal.repository.UserAuthRepository;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.rascal.filter.inteface.RefreshTokenProvider;
import id.rascal.filter.service.JwtService;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAuthRepository userAuthRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenProvider refreshTokenProvider;

    public AuthServiceImpl (
        JwtService jwtService,
        UserAuthRepository userAuthRepository,
        RefreshTokenRepository refreshTokenRepository,
        RefreshTokenProvider refreshTokenProvider,
        PasswordEncoder passwordEncoder
    ) { 
        this.jwtService = jwtService;
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenProvider = refreshTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public LoginResultResponse login(LoginRequest request) {
        UserAuth userAuth = userAuthRepository.findForLoginByEmail(request.email().toLowerCase().trim())
            .orElseThrow(() -> new BadRequestException("Username/password salah"));

        if (!passwordEncoder.matches(request.password(), userAuth.getHashedPassword()))
            throw new BadRequestException("Username/password salah");

        UserAuthData authData = getUserAuthData(userAuth);
        String accessToken = jwtService.generateToken(authData.userId(), authData.roles(), authData.authorities());
        String rawRefreshToken = refreshTokenProvider.generateToken();
        String hashRefreshToken = refreshTokenProvider.hash(rawRefreshToken);

        RefreshToken refreshToken = createRefreshToken(userAuth, hashRefreshToken);
        refreshTokenRepository.save(refreshToken);

        return new LoginResultResponse(
            userAuth.getId(), 
            userAuth.getEmail(), 
            accessToken, rawRefreshToken
        );
    }
    
    // TODO: add Unauthorized exception, refresh token rotation(delete readOnly)
    @Override
    @Transactional(readOnly = true)
    public RefreshResultResponse refresh(String rawRefreshToken) {
        String hashRefreshToken = refreshTokenProvider.hash(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashRefreshToken)
            .orElseThrow(() -> new NotFoundException("Token not found"));
        validateRefreshToken(refreshToken);

        UserAuth userAuth = refreshToken.getUserAuth();
        UserAuthData authData = getUserAuthData(userAuth);
        String accessToken = jwtService.generateToken(authData.userId(), authData.roles(), authData.authorities());
        
        return new RefreshResultResponse(
            userAuth.getId(),
            userAuth.getEmail(),
            accessToken,
            rawRefreshToken
        );
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = refreshTokenProvider.hash(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new NotFoundException("Token not found"));

        refreshToken.setRevokedAt(Instant.now());
    }

    @Override
    public void logoutAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    private record UserAuthData(
        String userId,
        Set<String> roles,
        Set<String> authorities
    ) { }

    private UserAuthData getUserAuthData(UserAuth userAuth) {
        String userId = userAuth.getId().toString();
        Set<String> roles = userAuth.getRoles().stream()
            .map(Role::getName).collect(Collectors.toSet());

        Set<String> authorities = userAuth.getRoles().stream()
            .flatMap(r -> r.getAuthorities().stream())
            .map(Authority::getName).collect(Collectors.toSet());

        return new UserAuthData(userId, roles, authorities);
    }

    private RefreshToken createRefreshToken(UserAuth userAuth, String hashToken) {
        RefreshToken refreshToken = new RefreshToken();
        Instant now = Instant.now();
        refreshToken.setTokenHash(hashToken);
        refreshToken.setUserAuth(userAuth);
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(now.plus(20, ChronoUnit.DAYS));
        refreshToken.setRevokedAt(null);

        return refreshToken;
    }

    private void validateRefreshToken(RefreshToken refreshToken) {
        Instant now = Instant.now();

        if (refreshToken.getExpiresAt().isBefore(now))
            throw new BadRequestException("Token expired");

        if (refreshToken.getRevokedAt() != null)
            throw new BadRequestException("Token revoked");
    }

}
