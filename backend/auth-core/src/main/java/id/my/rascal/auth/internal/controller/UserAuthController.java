package id.my.rascal.auth.internal.controller;

import id.my.rascal.auth.internal.model.request.UserAuthPatchRequest;
import id.my.rascal.auth.internal.model.request.UserAuthPutRequest;
import id.my.rascal.auth.internal.model.request.UserAuthRequest;
import id.my.rascal.auth.internal.model.response.UserAuthResponse;
import id.my.rascal.auth.internal.service.UserAuthService;
import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import jakarta.validation.Valid;
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
    public ResponseEntity<?> create(@RequestBody @Valid UserAuthRequest request) {
        UserAuthResponse data = userAuthService.create(request);
        return ApiResponse.success(HttpStatus.CREATED, "User created", data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        UserAuthResponse data = userAuthService.getById(id);
        return ApiResponse.success(HttpStatus.OK, data);
    }

    @GetMapping
    public ResponseEntity<?> getAll(
        @RequestParam(required = false) String email,
        Pageable pageable
    ) {
        Page<UserAuthResponse> page = userAuthService
            .searchActiveUsers(email, pageable);

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
    public ResponseEntity<?> updatePut(
        @PathVariable Long id,
        @RequestBody @Valid UserAuthPutRequest request
    ) {
        UserAuthResponse data = userAuthService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "User updated", data);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePatch(
        @PathVariable Long id,
        @RequestBody UserAuthPatchRequest request
    ) {
        if (request.isEmptyPatch())
            throw new BadRequestException("PATCH can't be empty");

        UserAuthResponse data = userAuthService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "User updated", data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (id <= 0)
            throw new BadRequestException("Invalid ID: " + id);

        userAuthService.delete(id);
        return ApiResponse.success(HttpStatus.OK, "User deleted", null);
    }
}
