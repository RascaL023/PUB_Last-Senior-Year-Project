package id.my.rascal.employee.internal.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import id.my.rascal.employee.internal.model.enums.EmployeeStatus;

public record EmployeeRequest(
    @NotBlank(message = "Employee name must be filled")
    @Size(min = 3, max = 100, message = "Employee name must be between 3 and 100 characters")
    String name,

    @NotBlank(message = "Email must be filled")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password must be filled")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "Role must be filled")
    String roleName,

    @Size(min = 10, max = 13, message = "Employee phone number must be between 10 and 13 digits")
    @Pattern(
        regexp = "^(\\+62|62|0)8[1-9][0-9]{10,13}$", 
        message = "Invalid phone number (e.g: 08123456789)"
    )
    String phone,

    EmployeeStatus status
) {}
