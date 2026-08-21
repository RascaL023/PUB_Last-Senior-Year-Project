package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.model.request.LoginRequest;
import id.my.rascal.auth.internal.model.response.LoginResultResponse;
import id.my.rascal.auth.internal.model.response.RefreshResultResponse;

public interface AuthService {

    LoginResultResponse login(LoginRequest request);
    RefreshResultResponse refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
    void logoutAll(Long userId);
    
}
