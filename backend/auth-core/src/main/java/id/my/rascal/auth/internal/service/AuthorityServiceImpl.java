package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.model.mapper.AuthorityMapper;
import id.my.rascal.auth.internal.model.request.AuthorityPatchRequest;
import id.my.rascal.auth.internal.model.request.AuthorityPutRequest;
import id.my.rascal.auth.internal.model.request.AuthorityRequest;
import id.my.rascal.auth.internal.model.response.AuthorityResponse;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import id.my.rascal.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthorityServiceImpl implements AuthorityService {

    private final AuthorityRepository authorityRepository;

    public AuthorityServiceImpl(AuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
    }

    @Override
    @Transactional
    public AuthorityResponse create(AuthorityRequest request) {
        Authority authority = AuthorityMapper.toEntity(request);
        authority = authorityRepository.save(authority);

        return AuthorityMapper.toResponse(authority);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorityResponse getById(Long id) {
        Authority authority = validateAndGetAuthorityById(id);
        return AuthorityMapper.toResponse(authority);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuthorityResponse> searchActiveAuthorities(
        String name,
        Pageable pageable
    ) {
        return authorityRepository
            .searchActive(normalizeSearchName(name), pageable)
            .map(AuthorityMapper::toResponse);
    }

    @Override
    @Transactional
    public AuthorityResponse update(Long id, AuthorityPutRequest request) {
        Authority authority = validateAndGetAuthorityById(id);
        AuthorityMapper.updateEntity(authority, request);
        authority = authorityRepository.save(authority);
        
        return AuthorityMapper.toResponse(authority);
    }

    @Override
    @Transactional
    public AuthorityResponse update(Long id, AuthorityPatchRequest request) {
        Authority authority = validateAndGetAuthorityById(id);
        AuthorityMapper.updateEntity(authority, request);
        authority = authorityRepository.save(authority);

        return AuthorityMapper.toResponse(authority);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Authority authority = validateAndGetAuthorityById(id);
        authority.setDeletedAt(LocalDateTime.now());
        authorityRepository.save(authority);
    }

    private Authority validateAndGetAuthorityById(Long id) {
        return authorityRepository.findActiveById(id)
            .orElseThrow(() ->
                new NotFoundException("Authority not found with id: " + id)
            );
    }

    private String normalizeSearchName(String name) {
        if (name == null || name.isBlank())
            return "";

        return name.trim();
    }

}
