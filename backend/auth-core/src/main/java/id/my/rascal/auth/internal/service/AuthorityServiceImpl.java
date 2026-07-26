package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.model.mapper.AuthorityMapper;
import id.my.rascal.auth.internal.model.request.AuthorityPatchRequest;
import id.my.rascal.auth.internal.model.request.AuthorityPutRequest;
import id.my.rascal.auth.internal.model.request.AuthorityRequest;
import id.my.rascal.auth.internal.model.response.AuthorityResponse;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
        Authority authority = authorityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Authority not found with id: " + id));
        return AuthorityMapper.toResponse(authority);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthorityResponse> getAll() {
        return authorityRepository.findAll().stream()
                .map(AuthorityMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AuthorityResponse update(Long id, AuthorityPutRequest request) {
        Authority authority = authorityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Authority not found with id: " + id));
        
        AuthorityMapper.updateEntity(authority, request);
        authority = authorityRepository.save(authority);
        return AuthorityMapper.toResponse(authority);
    }

    @Override
    @Transactional
    public AuthorityResponse update(Long id, AuthorityPatchRequest request) {
        Authority authority = authorityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Authority not found with id: " + id));
        
        AuthorityMapper.updateEntity(authority, request);
        authority = authorityRepository.save(authority);
        return AuthorityMapper.toResponse(authority);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Authority authority = authorityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Authority not found with id: " + id));
        authorityRepository.delete(authority);
    }
}
