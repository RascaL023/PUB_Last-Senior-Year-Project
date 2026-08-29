package id.my.rascal.auth.internal.controller;

import id.my.rascal.auth.internal.model.request.RolePatchRequest;
import id.my.rascal.auth.internal.model.request.RolePutRequest;
import id.my.rascal.auth.internal.model.request.RoleRequest;
import id.my.rascal.auth.internal.model.response.RoleResponse;
import id.my.rascal.auth.internal.service.RoleService;
import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.template.SuccessPagedTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auths/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('role.create', 'role.*')")
    public ResponseEntity<SuccessTemplate<RoleResponse>> create(@RequestBody @Valid RoleRequest request) {
        RoleResponse data = roleService.create(request);
        return ApiResponse.success(HttpStatus.CREATED, "Role successfully created", data);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role.read', 'role.*')")
    public ResponseEntity<SuccessTemplate<RoleResponse>> getById(@PathVariable Long id) {
        RoleResponse data = roleService.getById(id, false);
        return ApiResponse.success(HttpStatus.OK, "Role successfully retrieved", data);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('role.read', 'role.*')")
    public ResponseEntity<SuccessPagedTemplate<java.util.List<RoleResponse>>> getAll(
        @RequestParam(required = false) String name,
        Pageable pageable
    ) {
        Page<RoleResponse> page = roleService.searchRoles(name, false, pageable);

        return ApiResponse.paged(
            HttpStatus.OK, "Roles successfully retrieved",
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role.update', 'role.*')")
    public ResponseEntity<SuccessTemplate<RoleResponse>> updatePut(
        @PathVariable Long id, 
        @RequestBody @Valid RolePutRequest request
    ) {
        RoleResponse data = roleService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "Role successfully updated", data);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role.update', 'role.*')")
    public ResponseEntity<SuccessTemplate<RoleResponse>> updatePatch(
        @PathVariable Long id, 
        @RequestBody 
        RolePatchRequest request
    ) {
        if (request.isEmptyPatch()) 
            throw new BadRequestException("PATCH can't be empty");
        
        RoleResponse data = roleService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "Role successfully updated", data);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role.delete', 'role.*')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (id <= 0) 
            throw new BadRequestException("Invalid ID: " + id);

        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
