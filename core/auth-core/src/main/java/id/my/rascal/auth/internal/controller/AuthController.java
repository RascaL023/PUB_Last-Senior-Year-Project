package id.my.rascal.auth.internal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.auth.internal.model.request.LoginRequest;
import id.my.rascal.auth.internal.model.response.LoginResponse;
import id.my.rascal.auth.internal.service.AuthService;
import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessTemplate;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auths")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessTemplate<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK, 
            "Login success, welcome!", 
            authService.login(request)
        );
    }
    
}
