package id.my.rascal.auth.internal.controller;

import id.my.rascal.auth.internal.model.request.RolePatchRequest;
import id.my.rascal.auth.internal.model.request.RolePutRequest;
import id.my.rascal.auth.internal.model.request.RoleRequest;
import id.my.rascal.auth.internal.model.response.RoleResponse;
import id.my.rascal.auth.internal.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@RequestBody RoleRequest request) {
        return roleService.create(request);
    }

    @GetMapping("/{id}")
    public RoleResponse getById(@PathVariable Long id) {
        return roleService.getById(id);
    }

    @GetMapping
    public List<RoleResponse> getAll() {
        return roleService.getAll();
    }

    @PutMapping("/{id}")
    public RoleResponse updatePut(@PathVariable Long id, @RequestBody RolePutRequest request) {
        return roleService.update(id, request);
    }

    @PatchMapping("/{id}")
    public RoleResponse updatePatch(@PathVariable Long id, @RequestBody RolePatchRequest request) {
        return roleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }
}
