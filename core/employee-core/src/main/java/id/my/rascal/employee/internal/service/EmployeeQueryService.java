package id.my.rascal.employee.internal.service;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.employee.internal.entity.Employee;
import id.my.rascal.employee.internal.repository.EmployeeRepository;

@Service
public class EmployeeQueryService {

    private final EmployeeRepository employeeRepository;

    public EmployeeQueryService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public Employee findById(Long id) {
        return employeeRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Employee with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Employee findByUserAuthId(Long userAuthId) {
        return employeeRepository.findActiveByUserAuthId(userAuthId)
            .orElseThrow(() -> new NotFoundException("Employee with userAuthId " + userAuthId + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<Employee> findAllPaged(String keyword, Pageable pageable) {
        return employeeRepository.findByKeyword(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public List<Employee> findAllByIds(Collection<Long> ids) {
        return employeeRepository.findAllByIds(ids);
    }

}
