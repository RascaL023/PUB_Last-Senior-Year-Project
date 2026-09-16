package id.my.rascal.employee.internal.model.request;

import id.my.rascal.employee.internal.model.enums.EmployeeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeePutRequest(
    @NotBlank(message = "Employee name must be filled")
    @Size(min = 3, max = 100, message = "Employee name must be between 3 and 100 characters")
    String name,

    @Size(min = 10, max = 13, message = "Employee phone number must be between 10 and 13 digits")
    @Pattern(
        regexp = "^(\\+62|62|0)8[1-9][0-9]{10,13}$", 
        message = "Invalid phone number (e.g: 08123456789)"
    )
    String phone,

    EmployeeStatus status
) {}
