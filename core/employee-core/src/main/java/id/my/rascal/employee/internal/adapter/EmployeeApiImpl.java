package id.my.rascal.employee.internal.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.employee.api.EmployeeApi;
import id.my.rascal.employee.api.EmployeeApiResponse;
import id.my.rascal.employee.internal.model.mapper.EmployeeMapper;
import id.my.rascal.employee.internal.service.EmployeeQueryService;

@Component
public class EmployeeApiImpl implements EmployeeApi {

    private final EmployeeQueryService employeeQueryService;

    public EmployeeApiImpl(EmployeeQueryService employeeQueryService) {
        this.employeeQueryService = employeeQueryService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeApiResponse> getEmployeeSnapshots(Collection<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty())
            return List.of();

        return employeeQueryService.findAllByIds(employeeIds).stream()
            .map(EmployeeMapper::toApiResponse)
            .toList();
    }

}
