package id.my.rascal.auth.internal.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotEmpty(message = "Token must be filled")
    String token,

    @NotEmpty(message = "New password must be filled")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword
) {}
