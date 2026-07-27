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
import id.my.rascal.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

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
        Role role = RoleMapper.toEntity(request);
        
        if (request.authorityIds() != null && !request.authorityIds().isEmpty()) {
            Set<Authority> authorities = new HashSet<>(authorityRepository.findAllById(request.authorityIds()));
            role.setAuthorities(authorities);
        }
        
        role = roleRepository.save(role);
        return RoleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));
        return RoleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> getAllPaged(Pageable pageable) {
        return roleRepository.findAll(pageable).map(RoleMapper::toResponse);
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RolePutRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));
        
        RoleMapper.updateEntity(role, request);
        
        if (request.authorityIds() != null) {
            Set<Authority> authorities = new HashSet<>(authorityRepository.findAllById(request.authorityIds()));
            role.setAuthorities(authorities);
        } else {
            role.getAuthorities().clear();
        }
        
        role = roleRepository.save(role);
        return RoleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RolePatchRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));
        
        RoleMapper.updateEntity(role, request);
        
        if (request.authorityIds() != null && request.authorityIds().isPresent()) {
            Set<Authority> authorities = new HashSet<>(authorityRepository.findAllById(request.authorityIds().get()));
            role.setAuthorities(authorities);
        }
        
        role = roleRepository.save(role);
        return RoleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));
        roleRepository.delete(role);
    }
}
