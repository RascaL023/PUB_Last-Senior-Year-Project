package id.my.rascal.employee.internal.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.Authentication;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessPagedTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.employee.internal.model.enums.EmployeeStatus;
import id.my.rascal.employee.internal.model.request.EmployeePatchRequest;
import id.my.rascal.employee.internal.model.request.EmployeePutRequest;
import id.my.rascal.employee.internal.model.request.EmployeeRequest;
import id.my.rascal.employee.internal.model.response.EmployeeResponse;
import id.my.rascal.employee.internal.service.EmployeeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final String DEFAULT_GET_SUCCESS_MESSAGE = "Employee successfully retrieved";
    private final String DEFAULT_CREATE_SUCCESS_MESSAGE = "Employee successfully created";
    private final String DEFAULT_UPDATE_SUCCESS_MESSAGE = "Employee successfully updated";

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('employee.create', 'employee.*')")
    public ResponseEntity<SuccessTemplate<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            DEFAULT_CREATE_SUCCESS_MESSAGE,
            employeeService.create(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('employee.read', 'employee.*')")
    public ResponseEntity<SuccessPagedTemplate<List<EmployeeResponse>>> getAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) EmployeeStatus status,
        @RequestParam(defaultValue = "false") boolean includeDeleted,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<EmployeeResponse> page = employeeService.getAllPaged(keyword, status, includeDeleted, pageable);
        return ApiResponse.paged(
            HttpStatus.OK,
            DEFAULT_GET_SUCCESS_MESSAGE,
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SuccessTemplate<EmployeeResponse>> getMe(Authentication authentication) {
        Long userAuthId = Long.valueOf(authentication.getName());
        return ApiResponse.success(
            HttpStatus.OK,
            "Employee profile successfully retrieved",
            employeeService.getMe(userAuthId)
        );
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SuccessTemplate<EmployeeResponse>> updateMe(
        Authentication authentication,
        @Valid @RequestBody EmployeePutRequest request
    ) {
        Long userAuthId = Long.valueOf(authentication.getName());
        return ApiResponse.success(
            HttpStatus.OK,
            "Employee profile successfully updated",
            employeeService.updateMe(userAuthId, request)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('employee.read', 'employee.*')")
    public ResponseEntity<SuccessTemplate<EmployeeResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_GET_SUCCESS_MESSAGE,
            employeeService.getById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('employee.update', 'employee.*')")
    public ResponseEntity<SuccessTemplate<EmployeeResponse>> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody EmployeePutRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            employeeService.update(id, request)
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('employee.update', 'employee.*')")
    public ResponseEntity<SuccessTemplate<EmployeeResponse>> patch(
        @PathVariable("id") Long id,
        @RequestBody EmployeePatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            employeeService.patch(id, request)
        );
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyAuthority('employee.update', 'employee.*')")
    public ResponseEntity<SuccessTemplate<EmployeeResponse>> restore(
        @PathVariable("id") Long id
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            employeeService.restore(id)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('employee.delete', 'employee.*')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyAuthority('employee.update', 'employee.*')")
    public ResponseEntity<SuccessTemplate<EmployeeResponse>> activate(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Employee successfully activated",
            employeeService.activate(id)
        );
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyAuthority('employee.update', 'employee.*')")
    public ResponseEntity<SuccessTemplate<EmployeeResponse>> suspend(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Employee successfully suspended",
            employeeService.suspend(id)
        );
    }
}
