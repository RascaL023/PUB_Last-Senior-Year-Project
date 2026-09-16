package id.my.rascal.employee.internal.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.api.AuthApi;
import id.my.rascal.auth.api.CreateAccountRequest;
import id.my.rascal.auth.api.UserAuthApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.ConflictException;
import id.my.rascal.employee.internal.entity.Employee;
import id.my.rascal.employee.internal.model.enums.EmployeeStatus;
import id.my.rascal.employee.internal.model.mapper.EmployeeMapper;
import id.my.rascal.employee.internal.model.request.EmployeePatchRequest;
import id.my.rascal.employee.internal.model.request.EmployeePutRequest;
import id.my.rascal.employee.internal.model.request.EmployeeRequest;
import id.my.rascal.employee.internal.model.response.EmployeeResponse;
import id.my.rascal.employee.internal.repository.EmployeeRepository;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeQueryService employeeQueryService;
    private final AuthApi authApi;

    public EmployeeService(EmployeeRepository employeeRepository, EmployeeQueryService employeeQueryService, AuthApi authApi) {
        this.employeeRepository = employeeRepository;
        this.employeeQueryService = employeeQueryService;
        this.authApi = authApi;
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new ConflictException("Employee with email " + request.email() + " already exists");
        }

        UserAuthApiResponse auth = authApi.createAccount(
            new CreateAccountRequest(
                request.email(),
                request.password(),
                request.roleName().toUpperCase()
            )
        );

        Employee employee = new Employee();
        employee.setUserAuthId(auth.id());
        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setStatus(request.status() != null ? request.status() : EmployeeStatus.ACTIVE);
        employee.markActive();
        employee.setCreatedAt(LocalDateTime.now());

        Employee saved = employeeRepository.save(employee);
        return EmployeeMapper.toResponse(saved);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeePutRequest request) {
        Employee employee = employeeQueryService.findById(id);

        employee.setName(request.name());
        employee.setPhone(request.phone());
        if (request.status() != null) {
            employee.setStatus(request.status());
        }
        employee.setUpdatedAt(LocalDateTime.now());

        Employee updated = employeeRepository.save(employee);
        return EmployeeMapper.toResponse(updated);
    }

    @Transactional
    public EmployeeResponse patch(Long id, EmployeePatchRequest request) {
        if (request.isEmptyPatch()) throw new BadRequestException("PATCH cannot be empty!");
        Employee employee = employeeQueryService.findById(id);

        if (request.name() != null) employee.setName(request.name());
        if (request.phone() != null) employee.setPhone(request.phone());
        if (request.status() != null) employee.setStatus(request.status());
        employee.setUpdatedAt(LocalDateTime.now());

        Employee patched = employeeRepository.save(employee);
        return EmployeeMapper.toResponse(patched);
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = employeeQueryService.findById(id);
        if (employee.getUserAuthId() != null)
            authApi.softDeleteAccount(employee.getUserAuthId());
        employee.setDeletedAt(LocalDateTime.now());
        employeeRepository.save(employee);
    }

    @Transactional
    public EmployeeResponse restore(Long id) {
        Employee employee = employeeQueryService.findById(id);
        employee.setDeletedAt(null);
        employee.setUpdatedAt(LocalDateTime.now());
        Employee restored = employeeRepository.save(employee);
        return EmployeeMapper.toResponse(restored);
    }

    @Transactional
    public EmployeeResponse activate(Long id) {
        Employee employee = employeeQueryService.findById(id);
        employee.markActive();
        employee.setUpdatedAt(LocalDateTime.now());
        Employee updated = employeeRepository.save(employee);
        return EmployeeMapper.toResponse(updated);
    }

    @Transactional
    public EmployeeResponse suspend(Long id) {
        Employee employee = employeeQueryService.findById(id);
        employee.markSuspended();
        employee.setUpdatedAt(LocalDateTime.now());
        Employee updated = employeeRepository.save(employee);
        return EmployeeMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return EmployeeMapper.toResponse(employeeQueryService.findById(id));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllPaged(String keyword, Pageable pageable) {
        return employeeQueryService.findAllPaged(keyword, pageable).map(EmployeeMapper::toResponse);
    }

}
