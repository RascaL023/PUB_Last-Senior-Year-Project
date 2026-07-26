package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.model.request.AuthorityPatchRequest;
import id.my.rascal.auth.internal.model.request.AuthorityPutRequest;
import id.my.rascal.auth.internal.model.request.AuthorityRequest;
import id.my.rascal.auth.internal.model.response.AuthorityResponse;

import java.util.List;

public interface AuthorityService {
    AuthorityResponse create(AuthorityRequest request);
    AuthorityResponse getById(Long id);
    List<AuthorityResponse> getAll();
    AuthorityResponse update(Long id, AuthorityPutRequest request);
    AuthorityResponse update(Long id, AuthorityPatchRequest request);
    void delete(Long id);
}
