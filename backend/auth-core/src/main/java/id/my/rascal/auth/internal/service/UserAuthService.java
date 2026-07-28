package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.model.request.UserAuthPatchRequest;
import id.my.rascal.auth.internal.model.request.UserAuthPutRequest;
import id.my.rascal.auth.internal.model.request.UserAuthRequest;
import id.my.rascal.auth.internal.model.response.UserAuthResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAuthService {
    UserAuthResponse create(UserAuthRequest request);
    UserAuthResponse getById(Long id);
    UserAuthResponse getByEmail(String email);
    Page<UserAuthResponse> searchActiveUsers(String email, Pageable pageable);
    UserAuthResponse update(Long id, UserAuthPutRequest request);
    UserAuthResponse update(Long id, UserAuthPatchRequest request);
    void delete(Long id);
}
