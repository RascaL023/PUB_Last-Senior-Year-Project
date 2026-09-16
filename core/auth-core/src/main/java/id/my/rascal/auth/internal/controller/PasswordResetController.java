package id.my.rascal.auth.internal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.auth.internal.model.request.ForgotPasswordRequest;
import id.my.rascal.auth.internal.model.request.ResetPasswordRequest;
import id.my.rascal.auth.internal.model.response.ForgotPasswordResponse;
import id.my.rascal.auth.internal.model.response.ResetPasswordResponse;
import id.my.rascal.auth.internal.service.PasswordResetService;
import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessTemplate;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auths")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
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
