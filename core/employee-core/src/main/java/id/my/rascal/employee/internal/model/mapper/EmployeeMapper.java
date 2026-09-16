package id.my.rascal.employee.internal.model.mapper;

import id.my.rascal.employee.api.EmployeeApiResponse;
import id.my.rascal.employee.internal.entity.Employee;
import id.my.rascal.employee.internal.model.response.EmployeeResponse;

public class EmployeeMapper {

    private EmployeeMapper() { }

    public static EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
            employee.getId(),
            employee.getUserAuthId(),
            employee.getRoleName(),
            employee.getName(),
            employee.getEmail(),
            employee.getPhone(),
            employee.getStatus(),
            employee.getCreatedAt(),
            employee.getUpdatedAt(),
            employee.getDeletedAt()
        );
    }

    public static EmployeeApiResponse toApiResponse(Employee employee) {
        return new EmployeeApiResponse(
            employee.getId(),
            employee.getName(),
            employee.getEmail(),
            employee.getPhone()
        );
    }

}
