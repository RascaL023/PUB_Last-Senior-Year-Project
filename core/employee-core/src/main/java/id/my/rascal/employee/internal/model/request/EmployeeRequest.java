package id.my.rascal.employee.internal.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import id.my.rascal.employee.internal.model.enums.EmployeeStatus;

public record EmployeeRequest(
    @NotBlank(message = "Employee name must be filled")
    @Size(min = 3, max = 100, message = "Employee name must be between 3 and 100 characters")
    String name,

    @NotBlank(message = "Email must be filled")
    @Email(message = "Invalid email format")
    String email,

    String phone,

    @Size(max = 100, message = "Position must not exceed 100 characters")
    String position,

    @Size(max = 100, message = "Department must not exceed 100 characters")
    String department,

    EmployeeStatus status
) {}