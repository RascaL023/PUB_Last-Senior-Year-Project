package id.my.rascal.auth.internal.service;

import id.my.rascal.auth.internal.model.request.LoginRequest;
import id.my.rascal.auth.internal.model.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
    
}
