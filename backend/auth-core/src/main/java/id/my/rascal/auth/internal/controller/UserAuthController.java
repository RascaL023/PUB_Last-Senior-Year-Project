package id.my.rascal.auth.internal.controller;

import id.my.rascal.auth.internal.model.request.UserAuthPatchRequest;
import id.my.rascal.auth.internal.model.request.UserAuthPutRequest;
import id.my.rascal.auth.internal.model.request.UserAuthRequest;
import id.my.rascal.auth.internal.model.response.UserAuthResponse;
import id.my.rascal.auth.internal.service.UserAuthService;
import id.my.rascal.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserAuthController {

    private final UserAuthService userAuthService;

    public UserAuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody UserAuthRequest request) {
        UserAuthResponse data = userAuthService.create(request);
        return ApiResponse.success(HttpStatus.CREATED, "User created", data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        UserAuthResponse data = userAuthService.getById(id);
        return ApiResponse.success(HttpStatus.OK, data);
    }

    @GetMapping
    public ResponseEntity<?> getAll(Pageable pageable) {
        Page<UserAuthResponse> page = userAuthService.getAllPaged(pageable);
        return ApiResponse.paged(
            HttpStatus.OK, "Users retrieved",
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePut(@PathVariable Long id, @RequestBody UserAuthPutRequest request) {
        UserAuthResponse data = userAuthService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "User updated", data);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePatch(@PathVariable Long id, @RequestBody UserAuthPatchRequest request) {
        UserAuthResponse data = userAuthService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "User updated", data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        userAuthService.delete(id);
        return ApiResponse.success(HttpStatus.OK, "User deleted", null);
    }
}
