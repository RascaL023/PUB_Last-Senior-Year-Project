package id.my.rascal.auth.internal.adapter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.api.AuthApi;
import id.my.rascal.auth.api.CreateAccountRequest;
import id.my.rascal.auth.api.UserAuthApiResponse;
import id.my.rascal.auth.internal.entity.Role;
import id.my.rascal.auth.internal.entity.UserAuth;
import id.my.rascal.auth.internal.repository.RoleRepository;
import id.my.rascal.auth.internal.repository.UserAuthRepository;
import id.my.rascal.auth.internal.service.AuthService;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.ConflictException;
import id.my.rascal.common.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class AuthApiImpl implements AuthApi {

    private final UserAuthRepository userAuthRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public AuthApiImpl(
        UserAuthRepository userAuthRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        AuthService authService
    ) {
        this.userAuthRepository = userAuthRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
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

    @Override
    @Transactional
    public UserAuthApiResponse createAccount(CreateAccountRequest request) {
        if (request.email() == null || request.email().isBlank())
            throw new BadRequestException("Email is required");
        if (request.password() == null || request.password().length() < 8)
            throw new BadRequestException("Password must be at least 8 characters");
        if (userAuthRepository.existsByEmailAndDeletedAtIsNull(request.email()))
            throw new ConflictException("Email already exists: " + request.email());

        Role role = roleRepository.findByName(request.roleName())
            .filter(r -> r.getDeletedAt() == null)
            .orElseThrow(() -> new NotFoundException("Role not found: " + request.roleName()));

        UserAuth userAuth = new UserAuth();
        userAuth.setEmail(request.email());
        userAuth.setHashedPassword(passwordEncoder.encode(request.password()));
        userAuth.setCreatedAt(LocalDateTime.now());
        userAuth.setRoles(new HashSet<>(Set.of(role)));

        userAuth = userAuthRepository.save(userAuth);
        return new UserAuthApiResponse(userAuth.getId(), userAuth.getEmail());
    }

    @Override
    @Transactional
    public void softDeleteAccount(Long userAuthId) {
        if (userAuthId == null || userAuthId <= 0)
            throw new BadRequestException("Invalid userAuthId");
        userAuthRepository.findActiveById(userAuthId).ifPresent(userAuth -> {
            authService.logoutAll(userAuth.getId());
            userAuth.setDeletedAt(LocalDateTime.now());
            userAuthRepository.save(userAuth);
        });
    }

}
