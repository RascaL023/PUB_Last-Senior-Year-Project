package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.model.request.AuthorityPatchRequest;
import id.my.rascal.auth.internal.model.request.AuthorityPutRequest;
import id.my.rascal.auth.internal.model.request.AuthorityRequest;
import id.my.rascal.auth.internal.model.response.AuthorityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthorityService {
    AuthorityResponse create(AuthorityRequest request);
    AuthorityResponse getById(Long id);
    Page<AuthorityResponse> searchActiveAuthorities(String name, Pageable pageable);
    AuthorityResponse update(Long id, AuthorityPutRequest request);
    AuthorityResponse update(Long id, AuthorityPatchRequest request);
    void delete(Long id);
}
