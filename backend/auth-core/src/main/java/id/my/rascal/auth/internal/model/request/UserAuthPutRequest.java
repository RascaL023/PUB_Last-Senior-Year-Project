package id.my.rascal.auth.internal.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserAuthPutRequest(
    @Email(message = "Email must be a valid email address")
    @NotBlank(message = "Email is required")
    String email,

    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotEmpty(message = "Expected at least 1 role ID")
    Set<Long> roleIds
) {}
