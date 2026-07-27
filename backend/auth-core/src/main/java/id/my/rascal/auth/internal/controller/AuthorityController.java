package id.my.rascal.auth.internal.controller;

import id.my.rascal.auth.internal.model.request.AuthorityPatchRequest;
import id.my.rascal.auth.internal.model.request.AuthorityPutRequest;
import id.my.rascal.auth.internal.model.request.AuthorityRequest;
import id.my.rascal.auth.internal.model.response.AuthorityResponse;
import id.my.rascal.auth.internal.service.AuthorityService;
import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authorities")
public class AuthorityController {

    private final AuthorityService authorityService;

    public AuthorityController(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid AuthorityRequest request) {
        AuthorityResponse data = authorityService.create(request);
        return ApiResponse.success(HttpStatus.CREATED, "Authority created", data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        AuthorityResponse data = authorityService.getById(id);
        return ApiResponse.success(HttpStatus.OK, data);
    }

    @GetMapping
    public ResponseEntity<?> getAll(
        @RequestParam(required = false) String name,
        Pageable pageable
    ) {
        Page<AuthorityResponse> page = authorityService
            .searchActiveAuthorities(name, pageable);

        return ApiResponse.paged(
            HttpStatus.OK, "Authorities retrieved",
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
        @RequestBody @Valid AuthorityPutRequest request
    ) {
        AuthorityResponse data = authorityService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "Authority updated", data);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePatch(
        @PathVariable Long id,
        @RequestBody AuthorityPatchRequest request
    ) {
        if (request.isEmptyPatch())
            throw new BadRequestException("PATCH can't be empty");

        AuthorityResponse data = authorityService.update(id, request);
        return ApiResponse.success(HttpStatus.OK, "Authority updated", data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (id <= 0)
            throw new BadRequestException("Invalid ID: " + id);

        authorityService.delete(id);
        return ApiResponse.success(HttpStatus.OK, "Authority deleted", null);
    }

}
