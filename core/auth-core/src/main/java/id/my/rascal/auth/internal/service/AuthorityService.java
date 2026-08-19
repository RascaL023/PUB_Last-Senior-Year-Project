package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.model.response.AuthorityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthorityService {
    AuthorityResponse getById(Long id);
    Page<AuthorityResponse> searchActiveAuthorities(String name, Pageable pageable);
    void delete(Long id);
}
