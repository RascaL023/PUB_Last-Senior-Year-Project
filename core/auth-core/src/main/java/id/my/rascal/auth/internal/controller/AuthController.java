package id.my.rascal.auth.internal.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.auth.internal.model.request.ForgotPasswordRequest;
import id.my.rascal.auth.internal.model.request.LoginRequest;
import id.my.rascal.auth.internal.model.request.ResetPasswordRequest;
import id.my.rascal.auth.internal.model.response.ForgotPasswordResponse;
import id.my.rascal.auth.internal.model.response.LoginResponse;
import id.my.rascal.auth.internal.model.response.LoginResultResponse;
import id.my.rascal.auth.internal.model.response.RefreshResultResponse;
import id.my.rascal.auth.internal.model.response.ResetPasswordResponse;
import id.my.rascal.auth.internal.service.AuthService;
import id.my.rascal.auth.internal.service.PasswordResetService;
import id.my.rascal.auth.internal.service.RefreshTokenCookieFactory;
import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.UnauthorizedException;
import id.my.rascal.common.template.MetaTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auths")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;
    private final PasswordResetService passwordResetService;

    public AuthController(
        AuthService authService,
        RefreshTokenCookieFactory refreshTokenCookieFactory,
        PasswordResetService passwordResetService
    ) {
        this.authService = authService;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessTemplate<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        LoginResultResponse result = authService.login(request);
        ResponseCookie refreshCookie = refreshTokenCookieFactory
            .create(result.rawRefreshToken());

        LoginResponse response = new LoginResponse(
            result.userId(),
            result.email(),
            result.accessToken()
        );

        SuccessTemplate<LoginResponse> body = new SuccessTemplate<LoginResponse>(
            true, 
            "Login success, welcome back!", 
            response, 
            MetaTemplate.now()
        );

        return ResponseEntity
            .status(HttpStatus.OK)
            .header(
                HttpHeaders.SET_COOKIE, 
                refreshCookie.toString()
            ).body(body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
        @CookieValue(
            name = "refresh_token",
            required = false
        ) String rawRefreshToken
    ) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank())
            throw new UnauthorizedException("Refresh token is missing");

        RefreshResultResponse result = authService.refresh(rawRefreshToken);
        ResponseCookie refreshCookie = refreshTokenCookieFactory.create(result.rawRefreshToken());

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(ApiResponse.success(HttpStatus.OK, Map.of("accessToken", result.accessToken())).getBody());
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessTemplate<Void>> logout(
        @CookieValue(
            name = "refresh_token",
            required = false
        ) String rawRefreshToken
    ) {
        authService.logout(rawRefreshToken);

        ResponseCookie clearCookie = refreshTokenCookieFactory.clear();
        SuccessTemplate<Void> body = new SuccessTemplate<>(
            true,
            "Logout success",
            null,
            MetaTemplate.now()
        );

        return ResponseEntity.ok()
            .header(
                HttpHeaders.SET_COOKIE,
                clearCookie.toString()
            ).body(body);
    }

    @PostMapping("/logout-all")
    public ResponseEntity<SuccessTemplate<Void>> logoutAll(
        Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());
        authService.logoutAll(userId);
        
        ResponseCookie clearCookie = refreshTokenCookieFactory.clear();
        SuccessTemplate<Void> body = new SuccessTemplate<>(
            true,
            "Logout all success",
            null,
            MetaTemplate.now()
        );

        return ResponseEntity.ok()
            .header(
                HttpHeaders.SET_COOKIE,
                clearCookie.toString()
            ).body(body);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<SuccessTemplate<ForgotPasswordResponse>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request
    ) {
        ForgotPasswordResponse response = passwordResetService.requestReset(request);
        return ResponseEntity.ok(
            ApiResponse.success(HttpStatus.OK, "Request processed successfully", response).getBody()
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<SuccessTemplate<ResetPasswordResponse>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        ResetPasswordResponse response = passwordResetService.resetPassword(request);
        return ResponseEntity.ok(
            ApiResponse.success(HttpStatus.OK, "Request processed successfully", response).getBody()
        );
    }
    
}
