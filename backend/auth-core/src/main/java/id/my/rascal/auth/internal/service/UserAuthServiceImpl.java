package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.entity.Role;
import id.my.rascal.auth.internal.entity.UserAuth;
import id.my.rascal.auth.internal.model.mapper.UserAuthMapper;
import id.my.rascal.auth.internal.model.request.UserAuthPatchRequest;
import id.my.rascal.auth.internal.model.request.UserAuthPutRequest;
import id.my.rascal.auth.internal.model.request.UserAuthRequest;
import id.my.rascal.auth.internal.model.response.UserAuthResponse;
import id.my.rascal.auth.internal.repository.RoleRepository;
import id.my.rascal.auth.internal.repository.UserAuthRepository;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.ConflictException;
import id.my.rascal.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserAuthRepository userAuthRepository;
    private final RoleRepository roleRepository;

    public UserAuthServiceImpl(UserAuthRepository userAuthRepository, RoleRepository roleRepository) {
        this.userAuthRepository = userAuthRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public UserAuthResponse create(UserAuthRequest request) {
        if (userAuthRepository.findActiveByEmail(request.email()).isPresent())
            throw new ConflictException("Email already exists: " + request.email());

        UserAuth userAuth = UserAuthMapper.toEntity(request);

        if (request.roleIds() != null && !request.roleIds().isEmpty())
            userAuth.setRoles(resolveActiveRoles(request.roleIds()));

        userAuth = userAuthRepository.save(userAuth);
        return UserAuthMapper.toResponse(userAuth);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthResponse getById(Long id) {
        return UserAuthMapper.toResponse(validateAndGetUserById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthResponse getByEmail(String email) {
        UserAuth userAuth = userAuthRepository.findActiveByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
        return UserAuthMapper.toResponse(userAuth);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserAuthResponse> searchActiveUsers(String email, Pageable pageable) {
        return userAuthRepository
            .searchActive(normalizeSearchEmail(email), pageable)
            .map(UserAuthMapper::toResponse);
    }

    @Override
    @Transactional
    public UserAuthResponse update(Long id, UserAuthPutRequest request) {
        UserAuth userAuth = validateAndGetUserById(id);

        checkEmailConflict(userAuth.getEmail(), request.email());

        UserAuthMapper.updateEntity(userAuth, request);
        applyRoles(userAuth, request.roleIds());

        userAuth = userAuthRepository.save(userAuth);
        return UserAuthMapper.toResponse(userAuth);
    }

    @Override
    @Transactional
    public UserAuthResponse update(Long id, UserAuthPatchRequest request) {
        if (request.password().isPresent()) 
            rejectInvalidPasswordLength(request.password().get());

        UserAuth userAuth = validateAndGetUserById(id);

        if (request.email() != null && request.email().isPresent()) {
            String newEmail = request.email().get();
            validateEmailFormat(newEmail);
            checkEmailConflict(userAuth.getEmail(), newEmail);
        }

        if (request.password() != null && request.password().isPresent()
            && request.password().get().length() < 8)
            throw new BadRequestException("password must be at least 8 characters");

        UserAuthMapper.updateEntity(userAuth, request);

        if (request.roleIds() != null && request.roleIds().isPresent())
            userAuth.setRoles(resolveActiveRoles(request.roleIds().get()));

        userAuth = userAuthRepository.save(userAuth);
        return UserAuthMapper.toResponse(userAuth);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserAuth userAuth = validateAndGetUserById(id);
        userAuth.setDeletedAt(LocalDateTime.now());
        userAuthRepository.save(userAuth);
    }

    private UserAuth validateAndGetUserById(Long id) {
        return userAuthRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    private String normalizeSearchEmail(String email) {
        if (email == null || email.isBlank())
            return "";

        return email.trim();
    }

    private Set<Role> resolveActiveRoles(Set<Long> roleIds) {
        List<Role> roles = roleRepository.findAllById(roleIds);

        if (roles.size() != roleIds.size()) {
            Set<Long> foundIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toCollection(HashSet::new));

            Set<Long> missingIds = new HashSet<>(roleIds);
            missingIds.removeAll(foundIds);

            throw new NotFoundException("Not found IDs: " + missingIds);
        }

        return new HashSet<>(roles);
    }

    private void applyRoles(UserAuth userAuth, Set<Long> roleIds) {
        if (roleIds != null) {
            userAuth.setRoles(resolveActiveRoles(roleIds));
            return;
        }

        userAuth.getRoles().clear();
    }

    private void checkEmailConflict(String currentEmail, String newEmail) {
        if (!currentEmail.equals(newEmail)
            && userAuthRepository.findActiveByEmail(newEmail).isPresent())
            throw new ConflictException("Email already exists: " + newEmail);
    }

    private void validateEmailFormat(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches())
            throw new BadRequestException("email must be a valid email address");
    }

    private void rejectInvalidPasswordLength(String password) {
        if (password.length() < 8)
            throw new BadRequestException("Password at least have 8 characters");
    }

}
