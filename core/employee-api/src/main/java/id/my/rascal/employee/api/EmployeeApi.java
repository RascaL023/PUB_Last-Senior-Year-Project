package id.my.rascal.employee.api;

import java.util.Collection;
import java.util.List;

public interface EmployeeApi {
    List<EmployeeApiResponse> getEmployeeSnapshots(Collection<Long> employeeIds);
}