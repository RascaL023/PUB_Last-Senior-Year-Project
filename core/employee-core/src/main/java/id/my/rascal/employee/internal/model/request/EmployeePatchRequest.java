package id.my.rascal.employee.internal.model.request;

import id.my.rascal.employee.internal.model.enums.EmployeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record EmployeePatchRequest(
    @Size(max = 100, message = "Employee name must not exceed 100 characters")
    String name,

    @Email(message = "Invalid email format")
    String email,

    String phone,

    @Size(max = 100, message = "Position must not exceed 100 characters")
    String position,

    @Size(max = 100, message = "Department must not exceed 100 characters")
    String department,

    EmployeeStatus status
) {}