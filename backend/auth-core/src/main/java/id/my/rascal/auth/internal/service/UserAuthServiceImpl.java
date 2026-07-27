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
import id.my.rascal.common.exception.ConflictException;
import id.my.rascal.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    private final UserAuthRepository userAuthRepository;
    private final RoleRepository roleRepository;

    public UserAuthServiceImpl(UserAuthRepository userAuthRepository, RoleRepository roleRepository) {
        this.userAuthRepository = userAuthRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public UserAuthResponse create(UserAuthRequest request) {
        if (userAuthRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("Email already exists: " + request.email());
        }

        UserAuth userAuth = UserAuthMapper.toEntity(request);
        
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
            userAuth.setRoles(roles);
        }
        
        userAuth = userAuthRepository.save(userAuth);
        return UserAuthMapper.toResponse(userAuth);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthResponse getById(Long id) {
        UserAuth userAuth = userAuthRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        return UserAuthMapper.toResponse(userAuth);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthResponse getByEmail(String email) {
        UserAuth userAuth = userAuthRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
        return UserAuthMapper.toResponse(userAuth);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserAuthResponse> getAllPaged(Pageable pageable) {
        return userAuthRepository.findAll(pageable).map(UserAuthMapper::toResponse);
    }

    @Override
    @Transactional
    public UserAuthResponse update(Long id, UserAuthPutRequest request) {
        UserAuth userAuth = userAuthRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        
        if (!userAuth.getEmail().equals(request.email()) && userAuthRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("Email already exists: " + request.email());
        }

        UserAuthMapper.updateEntity(userAuth, request);
        
        if (request.roleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
            userAuth.setRoles(roles);
        } else {
            userAuth.getRoles().clear();
        }
        
        userAuth = userAuthRepository.save(userAuth);
        return UserAuthMapper.toResponse(userAuth);
    }

    @Override
    @Transactional
    public UserAuthResponse update(Long id, UserAuthPatchRequest request) {
        UserAuth userAuth = userAuthRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        
        if (request.email() != null && request.email().isPresent() && !userAuth.getEmail().equals(request.email().get())) {
            if (userAuthRepository.findByEmail(request.email().get()).isPresent()) {
                throw new ConflictException("Email already exists: " + request.email().get());
            }
        }

        UserAuthMapper.updateEntity(userAuth, request);
        
        if (request.roleIds() != null && request.roleIds().isPresent()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds().get()));
            userAuth.setRoles(roles);
        }
        
        userAuth = userAuthRepository.save(userAuth);
        return UserAuthMapper.toResponse(userAuth);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserAuth userAuth = userAuthRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        userAuthRepository.delete(userAuth);
    }
}
