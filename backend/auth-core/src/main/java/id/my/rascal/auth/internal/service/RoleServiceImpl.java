package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.entity.Role;
import id.my.rascal.auth.internal.model.mapper.RoleMapper;
import id.my.rascal.auth.internal.model.request.RolePatchRequest;
import id.my.rascal.auth.internal.model.request.RolePutRequest;
import id.my.rascal.auth.internal.model.request.RoleRequest;
import id.my.rascal.auth.internal.model.response.RoleResponse;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import id.my.rascal.auth.internal.repository.RoleRepository;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;

    public RoleServiceImpl(RoleRepository roleRepository, AuthorityRepository authorityRepository) {
        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
    }

    @Override
    @Transactional
    public RoleResponse create(RoleRequest request) {
        List<Authority> authorities = authorityRepository.findAllById(request.authorityIds());
        validateAuthorityIds(authorities, request.authorityIds());

        Role role = RoleMapper.toEntity(request);
        role.setAuthorities(new HashSet<>(authorities));
        role = roleRepository.save(role);

        return RoleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        Role role = validateAndGetRoleById(id);
        return RoleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> searchActiveRoles(String name, Pageable pageable) {
        Page<Role> roles = roleRepository.searchActiveRole(normalizeSearchName(name), pageable);
        return roles.map(RoleMapper::toResponse);
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RolePutRequest request) {
        Role role = validateAndGetRoleById(id);
        RoleMapper.updateEntity(role, request);
        
        if (request.authorityIds() != null) {
            List<Authority> authorities = authorityRepository.findAllById(request.authorityIds());
            validateAuthorityIds(authorities, request.authorityIds());

            role.setAuthorities(new HashSet<>(authorities));
        } else role.getAuthorities().clear();
        
        role = roleRepository.save(role);
        return RoleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RolePatchRequest request) {
        if (request.name().isPresent()) 
            rejectInvalidLengthPatchName(request.name().get());

        Role role = validateAndGetRoleById(id);
        RoleMapper.updateEntity(role, request);
        
        if (request.authorityIds() != null && request.authorityIds().isPresent()) {
            List<Authority> authorities = authorityRepository
                .findAllById(request.authorityIds().get());
            validateAuthorityIds(authorities, request.authorityIds().get());

            role.setAuthorities(new HashSet<>(authorities));
        }
        
        role = roleRepository.save(role);
        return RoleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = validateAndGetRoleById(id);
        role.setDeletedAt(LocalDateTime.now());
        roleRepository.save(role);
    }


    private void validateAuthorityIds(List<Authority> authorities, Set<Long> requestIds) {
        if (authorities.size() != requestIds.size()) {
            Set<Long> foundIds = authorities.stream()
            .map(Authority::getId)
            .collect(Collectors.toCollection(HashSet::new));

            Set<Long> missingIds = new HashSet<>(requestIds);
            missingIds.removeAll(foundIds);

            throw new NotFoundException("Not found IDs: " + missingIds);
        }
    }

    private Role validateAndGetRoleById(Long id) {
        return roleRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));
    }

    private String normalizeSearchName(String name) {
        if (name == null || name.isBlank())
            return "";

        return name.trim();
    }

    private void rejectInvalidLengthPatchName(String name) {
        int length = name.length();
        if (length < 3 || length > 20)
            throw new BadRequestException("Valid role name are between 3 to 20 characters");
    }

}
