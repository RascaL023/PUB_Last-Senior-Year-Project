package id.my.rascal.auth.internal.controller;

import id.my.rascal.auth.internal.model.request.UserAuthPatchRequest;
import id.my.rascal.auth.internal.model.request.UserAuthPutRequest;
import id.my.rascal.auth.internal.model.request.UserAuthRequest;
import id.my.rascal.auth.internal.model.response.UserAuthResponse;
import id.my.rascal.auth.internal.service.UserAuthService;
import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.template.SuccessPagedTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auths/users")
public class UserAuthController {

    private final UserAuthService userAuthService;

    public UserAuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping
    public ResponseEntity<SuccessTemplate<UserAuthResponse>> create(@RequestBody @Valid UserAuthRequest request) {
        UserAuthResponse data = userAuthService.create(request);
        return ApiResponse.success(HttpStatus.CREATED, "User successfully created", data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessTemplate<UserAuthResponse>> getById(@PathVariable Long id) {
        UserAuthResponse data = userAuthService.getById(id);
        return ApiResponse.success(HttpStatus.OK, "User successfully retrieved", data);
    }

    @GetMapping
    public ResponseEntity<SuccessPagedTemplate<java.util.List<UserAuthResponse>>> getAll(
        @RequestParam(required = false) String email,
        Pageable pageable
    ) {
        Page<UserAuthResponse> page = userAuthService
            .searchActiveUsers(email, pageable);

        return ApiResponse.paged(
            HttpStatus.OK, "Users successfully retrieved",
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessTemplate<UserAuthResponse>> updatePut(
        @PathVariable Long id,
        @RequestBody @Valid UserAuthPutRequest request
    ) {
        UserAuthResponse data = userAuthService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "User successfully updated", data);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SuccessTemplate<UserAuthResponse>> updatePatch(
        @PathVariable Long id,
        @RequestBody UserAuthPatchRequest request
    ) {
        if (request.isEmptyPatch())
            throw new BadRequestException("PATCH can't be empty");

        UserAuthResponse data = userAuthService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "User successfully updated", data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (id <= 0)
            throw new BadRequestException("Invalid ID: " + id);

        userAuthService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
