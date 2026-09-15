package id.my.rascal.employee.internal.model.response;

import java.time.LocalDateTime;

import id.my.rascal.employee.internal.model.enums.EmployeeStatus;

public record EmployeeResponse(
    Long id,
    String name,
    String email,
    String phone,
    String position,
    String department,
    EmployeeStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {}