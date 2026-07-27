package id.my.rascal.auth.internal.controller;

import id.my.rascal.auth.internal.model.request.RolePatchRequest;
import id.my.rascal.auth.internal.model.request.RolePutRequest;
import id.my.rascal.auth.internal.model.request.RoleRequest;
import id.my.rascal.auth.internal.model.response.RoleResponse;
import id.my.rascal.auth.internal.service.RoleService;
import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid RoleRequest request) {
        RoleResponse data = roleService.create(request);
        return ApiResponse.success(HttpStatus.CREATED, "Role created", data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        RoleResponse data = roleService.getById(id);
        return ApiResponse.success(HttpStatus.OK, data);
    }

    @GetMapping
    public ResponseEntity<?> getAll(
        @RequestParam(required = false) String name,
        Pageable pageable
    ) {
        Page<RoleResponse> page = roleService.searchActiveRoles(name, pageable);

        return ApiResponse.paged(
            HttpStatus.OK, "Roles retrieved",
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePut(
        @PathVariable Long id, 
        @RequestBody @Valid RolePutRequest request
    ) {
        RoleResponse data = roleService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "Role updated", data);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePatch(
        @PathVariable Long id, 
        @RequestBody 
        RolePatchRequest request
    ) {
        if (request.isEmptyPatch()) 
            throw new BadRequestException("PATCH can't be empty");
        
        RoleResponse data = roleService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "Role updated", data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (id <= 0) 
            throw new BadRequestException("Invalid ID: " + id);

        roleService.delete(id);
        return ApiResponse.success(HttpStatus.OK, "Role deleted", null);
    }

}
