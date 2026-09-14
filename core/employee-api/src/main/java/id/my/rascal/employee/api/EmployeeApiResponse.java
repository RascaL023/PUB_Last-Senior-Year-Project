package id.my.rascal.employee.api;

public record EmployeeApiResponse(
    Long id,
    String name,
    String email,
    String phone,
    String position,
    String department
) {}