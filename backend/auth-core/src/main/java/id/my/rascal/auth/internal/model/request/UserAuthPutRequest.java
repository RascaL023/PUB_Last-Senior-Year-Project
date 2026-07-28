package id.my.rascal.auth.internal.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserAuthPutRequest(
    @Email(message = "email must be a valid email address")
    @NotBlank(message = "email is required")
    String email,

    @Size(min = 8, message = "password must be at least 8 characters")
    String password,

    Set<Long> roleIds
) {}
